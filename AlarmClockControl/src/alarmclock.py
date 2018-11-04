#!/usr/bin/env python
# encoding: utf-8
'''
alarmclock -- shortdesc

alarmclock is a script to control an Arduino/Raspberry Pi alarm clock.

@author:     camrdale

@copyright:  2018 camrdale. All rights reserved.

@license:    Apache License 2.0

@contact:    camrdale@gmail.com
'''

import datetime
import json
import mpd
import random
import sys
import textwrap
import traceback

from argparse import ArgumentParser
from argparse import RawDescriptionHelpFormatter

from crontab import CronTab

from twisted.internet import reactor
from twisted.internet import task
from twisted.internet.defer import succeed
from twisted.internet.serialport import SerialPort
from twisted.protocols.basic import LineReceiver
from twisted.python import log
from twisted.web.client import Agent
from twisted.web.client import readBody
from twisted.web.http_headers import Headers
from twisted.web.iweb import IBodyProducer

from zope.interface import implementer  # @UnresolvedImport

__all__ = []
__version__ = 0.1
__date__ = '2018-01-13'
__updated__ = '2018-01-13'

DEBUG = 0
TESTRUN = 0
PROFILE = 0
KQED = 'https://streams2.kqed.org/kqedradio'
NUM_ALARMS_DISPLAY = 10
CLOCK_KEY_DELIM = 'clock_key='
VERIFICATION_NUMBER_DELIM = 'verify='
DEFAULT_CHECKIN_PERIOD = 60.0
REGISTERING_CHECKIN_PERIOD = 5.0

# Buttons
SLEEP_BUTTON = b'L'
SNOOZE_BUTTON = b'R'
REGISTER_BUTTON = b'LR'

# States
OFF = 1
SLEEP = 2
ALARM = 3
SNOOZE = 4


def mpdConnect():
    client = mpd.MPDClient()
    client.timeout = 10
    client.idletimeout = None
    client.connect('localhost', 6600)
    print(client.mpd_version)
    print(client.status())
    return client


def playKqed(client):
    client.clear()
    client.add(KQED)
    client.play()
    print(client.status())
    print(client.playlist())


def stopPlaying(client):
    client.stop()
    print(client.status())


def mpdClose(client):
    client.close()
    client.disconnect()


class Alarm(object):
    '''A single (possibly recurring) alarm.'''

    def __init__(self, crontab, buzzer):
        self._crontab = crontab
        self._buzzer = buzzer
        self._alarm = CronTab(crontab)

    @classmethod
    def fromSaveString(cls, line):
        buzzer = False
        pieces = line.split(',')
        crontab = pieces[0].strip()
        if len(pieces) > 1:
            if pieces[1].strip() != '0':
                buzzer = True
        return cls(crontab, buzzer)

    @classmethod
    def fromJson(cls, json):
        return cls(json['crontab'], json.get('buzzer', False))
    
    def toSaveString(self):
        return self._crontab + "," + ("1" if self._buzzer else "0")

    def toJson(self):
        return {'crontab': self._crontab, 'buzzer': self._buzzer} 
       
    def next(self, now=None):
        if now is None:
            return self._alarm.next(default_utc=False)
        else:
            return self._alarm.next(now=now, default_utc=False)

    def get_buzzer(self):
        return self._buzzer


class Alarms(object):
    '''Container for multiple recurring alarms.'''

    def __init__(self, save_path):
        self._save_path = None
        self._alarms = []
        self._webKey = None
        if save_path != '':
            self._save_path = save_path
            try:
                with open(self._save_path) as f:
                    for line in f.readlines():
                        if line.startswith(CLOCK_KEY_DELIM):
                            self._webKey = line[len(CLOCK_KEY_DELIM):].strip()
                            continue
                        if line.startswith('#'):
                            continue
                        if line.strip():
                            self._alarms.append(Alarm.fromSaveString(line.strip()))
            except (OSError, IOError):
                sys.stdout.write('Ignoring missing save file: %s\n' % self._save_path)
            else:
                sys.stdout.write('Loaded %s alarms from save file %s:\n  %s\n' %
                                 (len(self._alarms), self._save_path,
                                  '\n  '.join(alarm.toSaveString() for alarm in self._alarms)))

    def reschedule_all(self, alarms):
        self._alarms = [Alarm.fromJson(json) for json in alarms]
        sys.stdout.write('Rescheduling new alarms:\n  %s\n' %
                         ('\n  '.join(alarm.toSaveString() for alarm in self._alarms),))
        self._save_to_file()
        
    def set_web_key(self, webKey):
        self._webKey = webKey
        self._save_to_file()

    def _save_to_file(self):
        if self._save_path:
            try:
                with open(self._save_path, 'w') as f:
                    if self._webKey:
                        f.write(CLOCK_KEY_DELIM + self._webKey + '\n')
                    f.write('# m h dom mon dow , buzzer\n')
                    for alarm in self._alarms:
                        f.write(alarm.toSaveString() + '\n')
                sys.stdout.write('Wrote %s alarms to save file %s\n' %
                                 (len(self._alarms), self._save_path))
            except Exception:
                traceback.print_exc()
                sys.stderr.write('Failed to write to save file: %s\n' % self._save_path)

    def next_alarm(self):
        _next_alarms = [(alarm.next(), alarm) for alarm in self._alarms]
        if not _next_alarms:
            return None
        _next_alarm = min(_next_alarms)
        return _next_alarm[1]

    def next_alarms(self, num_alarms, now=None):
        if now is None:
            now = datetime.datetime.now()
        result = []

        for _ in range(num_alarms):
            _next_alarms = [alarm.next(now=now) for alarm in self._alarms]
            if not _next_alarms:
                break
            now += datetime.timedelta(seconds=min(_next_alarms))
            result.append(now)

        return result

    def num_alarms(self):
        return len(self._alarms)

    def get_alarm_json(self):
        return [alarm.toJson() for alarm in self._alarms]

    def get_web_key(self):
        return self._webKey


class SerialProtocol(LineReceiver):
    '''Arduino serial communication protocol.'''

    def __init__(self, mpd, alarms):
        self.mpd = mpd
        self.alarms = alarms
        self.state = OFF
        self.relay = False
        self.buzzer = False
        self.lights = False
        self.sleep_time = None
        self.snooze_time = None
        self.alarm_off_time = None
        self.alarm_time = None
        self.next_alarm = None
        self.verification_number = None
        self.verification_time = None
        self._web = None
        
        loop = task.LoopingCall(self.sendState)

        def cbLoopDone(result):
            '''Called when loop was stopped with success.'''
            print('Loop done: ' + result)

        def ebLoopFailed(failure):
            '''Called when loop execution failed.'''
            print(failure.getBriefTraceback())

        # Start looping every 10 seconds.
        loopDeferred = loop.start(10.0)

        # Add callbacks for stop and failure.
        loopDeferred.addCallback(cbLoopDone)
        loopDeferred.addErrback(ebLoopFailed)

    def setWebInterface(self, web):
        self._web = web

    def connectionMade(self):
        print('Serial port connected.')
        self.rescheduleAlarm()
        self.sendState()

    def rescheduleAlarm(self):
        if self.alarm_time is not None and self.alarm_time.active():
            self.alarm_time.cancel()
        self.next_alarm = self.alarms.next_alarm()
        if self.next_alarm is not None:
            next_time = self.next_alarm.next()
            sys.stdout.write('Scheduling next alarm in: ' + str(next_time) + ' seconds\n')
            self.alarm_time = reactor.callLater(next_time, self.alarm_sounds)  # @UndefinedVariable
        else:
            sys.stdout.write('No alarms to schedule\n')

    def lineReceived(self, line):
        print('Serial RX: {0}'.format(line))
        if line[:2] == REGISTER_BUTTON:
            if self.verification_time is not None and self.verification_time.active():
                self.verification_time.cancel()
            self.verification_number = str(random.randint(0, 9999)).zfill(4)
            sys.stdout.write('Generated verification number: ' + self.verification_number + '\n')
            if self._web:
                self._web.register(self.verification_number)
            self.verification_time = reactor.callLater(60.0, self.finishedVerifying)  # @UndefinedVariable
            sys.stdout.write('Starting registration\n')
            self.sendState()

        elif line[:1] == SLEEP_BUTTON:
            if self.state == OFF:
                self.state = SLEEP
                self.sleep_time = reactor.callLater(3600.0, self.everything_off)  # @UndefinedVariable
                self.relay = True
                self.buzzer = False
                self.lights = False
                sys.stdout.write('Turning on sleep\n')
                self.sendState()
                if (self.mpd):
                    client = mpdConnect()
                    playKqed(client)
                    mpdClose(client)
            elif self.state == SLEEP:
                if self.sleep_time is not None and self.sleep_time.active():
                    self.sleep_time.cancel()
                self.sleep_time = reactor.callLater(3600.0, self.everything_off)  # @UndefinedVariable
            elif self.state == ALARM or self.state == SNOOZE:
                self.state = OFF
                if self.alarm_off_time is not None and self.alarm_off_time.active():
                    self.alarm_off_time.cancel()
                self.alarm_off_time = None
                if self.snooze_time is not None and self.snooze_time.active():
                    self.snooze_time.cancel()
                self.snooze_time = None
                self.relay = False
                self.buzzer = False
                self.lights = False
                sys.stdout.write('Turning off alarm\n')
                self.sendState()
                if (self.mpd):
                    client = mpdConnect()
                    stopPlaying(client)
                    mpdClose(client)

        elif line[:1] == SNOOZE_BUTTON:
            if self.state == ALARM:
                self.state = SNOOZE
                if self.snooze_time is not None and self.snooze_time.active():
                    self.snooze_time.cancel()
                self.snooze_time = reactor.callLater(540.0, self.snooze_over)  # @UndefinedVariable
                self.relay = False
                self.buzzer = False
                self.lights = False
                sys.stdout.write('Turning on snooze\n')
                self.sendState()
                if (self.mpd):
                    client = mpdConnect()
                    stopPlaying(client)
                    mpdClose(client)
            elif self.state == SNOOZE:
                if self.snooze_time is not None and self.snooze_time.active():
                    self.snooze_time.cancel()
                self.snooze_time = reactor.callLater(540.0, self.snooze_over)  # @UndefinedVariable
            elif self.state == SLEEP:
                self.state = OFF
                if self.sleep_time is not None and self.sleep_time.active():
                    self.sleep_time.cancel()
                self.sleep_time = None
                self.relay = False
                self.buzzer = False
                self.lights = False
                sys.stdout.write('Turning off sleep\n')
                self.sendState()
                if (self.mpd):
                    client = mpdConnect()
                    stopPlaying(client)
                    mpdClose(client)

    def finishedVerifying(self):
        if self.verification_time is not None and self.verification_time.active():
            self.verification_time.cancel()
        if self.verification_number:
            self.verification_number = None
            sys.stdout.write('Resuming after registration\n')
            self.sendState()
        if self._web:
            self._web.doneRegistering()
            
    def isVerifying(self):
        return bool(self.verification_number)

    def alarm_sounds(self):
        self.rescheduleAlarm()
        if self.state != OFF:
            sys.stderr.write('Bad state ' + str(self.state) + ', expected ' + str(OFF))
            return
        self.state = ALARM
        self.alarm_off_time = reactor.callLater(3600.0, self.everything_off)  # @UndefinedVariable
        self.relay = True
        self.buzzer = self.next_alarm.get_buzzer()
        self.lights = True
        sys.stdout.write('Turning on alarm\n')
        self.sendState()
        if (self.mpd):
            client = mpdConnect()
            playKqed(client)
            mpdClose(client)

    def snooze_over(self):
        if self.state != SNOOZE:
            sys.stderr.write('Bad state ' + str(self.state) + ', expected ' + str(SNOOZE))
            return
        self.state = ALARM
        self.snooze_time = None
        self.relay = True
        self.buzzer = self.next_alarm.get_buzzer()
        self.lights = True
        sys.stdout.write('Resuming alarm after snooze\n')
        self.sendState()
        if (self.mpd):
            client = mpdConnect()
            playKqed(client)
            mpdClose(client)

    def everything_off(self):
        if self.state == SLEEP:
            self.sleep_time = None
            sys.stdout.write('Turning off radio after sleep\n')
        elif self.state == SNOOZE:
            if self.snooze_time is not None and self.snooze_time.active():
                self.snooze_time.cancel()
            self.snooze_time = None
            self.alarm_off_time = None
            sys.stdout.write('Turning off alarm\n')
        elif self.state == ALARM:
            self.alarm_off_time = None
            sys.stdout.write('Turning off alarm\n')
        else:
            sys.stderr.write('Bad state ' + str(self.state) + ', expected ' + str(SLEEP))
            return
        self.state = OFF
        self.relay = False
        self.buzzer = False
        self.lights = False
        self.sendState()
        if (self.mpd):
            client = mpdConnect()
            stopPlaying(client)
            mpdClose(client)

    def sendState(self):
        if self.verification_number:
            new_time = VERIFICATION_NUMBER_DELIM + self.verification_number + '\n'
        else:
            new_time = datetime.datetime.now().strftime('%Y%m%d%H%M%S') + str(int(self.relay)) + str(int(self.buzzer)) + str(int(self.lights)) + '\n'
        sys.stdout.write('Sending time: ' + new_time)
        if self.transport:
            self.transport.write(new_time.encode('utf-8'))


@implementer(IBodyProducer)
class BytesProducer(object):
    def __init__(self, body):
        self.body = body
        self.length = len(body)

    def startProducing(self, consumer):
        consumer.write(self.body)
        return succeed(None)

    def pauseProducing(self):
        pass

    def stopProducing(self):
        pass
    
    
class WebInterface(object):

    def __init__(self, web, agent, alarms, serialProtocol):
        self._web = web.encode('utf-8')
        self._agent = agent
        self._alarms = alarms
        self._serialProtocol = serialProtocol
        self._lastRevision = None
        self._loop = None
        self._startLooping(DEFAULT_CHECKIN_PERIOD)

    def _startLooping(self, period):
        if self._loop:
            self._loop.stop()
        self._loop = task.LoopingCall(self.checkIn)

        # Start looping every 60 seconds.
        self._loop.start(period)

    def checkIn(self):
        if not self._alarms.get_web_key():
            return
        data = {}
        data['clock_key'] = self._alarms.get_web_key()

        print('Sending JSON: ' + str(data))
        content = json.dumps(data)
        print('Sending content: ' + content)

        d = self._agent.request(
            b'POST',
            self._web + b'/checkin',
            Headers({'Content-Type': ['application/json']}),
            BytesProducer(content.encode('utf-8')))

        d.addCallback(self._checkInResponse)

    def _checkInResponse(self, response):
        print('Response version:', response.version)
        print('Response code:', response.code)
        print('Response phrase:', response.phrase)
        print('Response headers:')
        print(list(response.headers.getAllRawHeaders()))
        d = readBody(response)
        d.addCallback(self._checkInBody)
        return d

    def _checkInBody(self, body):
        content = body.decode('utf-8')
        print('Received content: ' + content)
        inputData = json.loads(content)
        print('Received JSON: ' + str(inputData))

        if 'revision' in inputData:
            if inputData['revision'] == self._lastRevision:
                print('Ignoring already seen content')
                return
        
        if inputData.get('claimed', False):
            if self._serialProtocol.isVerifying():
                self._serialProtocol.finishedVerifying()
            if 'alarms' in inputData:
                self._alarms.reschedule_all(inputData['alarms'])
                self._serialProtocol.rescheduleAlarm()

        if 'revision' in inputData:
            self._lastRevision = inputData['revision']

    def register(self, verification_number):
        data = {}
        data['verification_number'] = verification_number

        print('Sending JSON: ' + str(data))
        content = json.dumps(data)
        print('Sending content: ' + content)


        d = self._agent.request(
            b'POST',
            self._web + b'/register',
            Headers({'Content-Type': ['application/json']}),
            BytesProducer(content.encode('utf-8')))

        d.addCallback(self._registerResponse)

    def _registerResponse(self, response):
        print('Response version:', response.version)
        print('Response code:', response.code)
        print('Response phrase:', response.phrase)
        print('Response headers:')
        print(list(response.headers.getAllRawHeaders()))
        d = readBody(response)
        d.addCallback(self._registerBody)
        return d

    def _registerBody(self, body):
        content = body.decode('utf-8')
        print('Received content: ' + content)
        inputData = json.loads(content)
        print('Received JSON: ' + str(inputData))

        if 'clock_key' in inputData:
            self._alarms.set_web_key(inputData['clock_key'])
            self._startLooping(REGISTERING_CHECKIN_PERIOD)

    def doneRegistering(self):
        self._startLooping(DEFAULT_CHECKIN_PERIOD)

def main(argv=None):  # IGNORE:C0111
    '''Command line options.'''

    if argv is None:
        argv = sys.argv
    else:
        sys.argv.extend(argv)

    log.startLogging(sys.stdout)

    program_version = 'v%s' % __version__
    program_build_date = str(__updated__)
    program_version_message = '%%(prog)s %s (%s)' % (program_version, program_build_date)
    program_shortdesc = __import__('__main__').__doc__.split('\n')[1]
    program_license = textwrap.dedent('''\
    %s

      Created by camrdale on %s.
      Copyright 2018 camrdale. All rights reserved.

      Licensed under the Apache License 2.0
      http://www.apache.org/licenses/LICENSE-2.0

      Distributed on an "AS IS" basis without warranties
      or conditions of any kind, either express or implied.

    USAGE
    ''' % (program_shortdesc, str(__date__)))

    try:
        parser = ArgumentParser(description=program_license, formatter_class=RawDescriptionHelpFormatter)
        parser.add_argument('-s', '--save', dest='save', type=str, default='alarmclock.crontab', help='if set, file to save alarms to [default: %(default)s]')
        parser.add_argument('-m', '--mpd', dest='mpd', action='store_true', default=False, help='enable MPD server [default: %(default)s]')
        parser.add_argument('-v', '--verbose', dest='verbose', action='count', help='set verbosity level [default: %(default)s]')
        parser.add_argument('-V', '--version', action='version', version=program_version_message)
        parser.add_argument('--web', type=str, default='http://localhost:8080',
                            help='Web server to contact to retrieve alarms. Use 0 to disable.')
        parser.add_argument(dest='port', help='serial port to connect to [default: %(default)s]', nargs='?', default='COM3')

        args = parser.parse_args()

        print('Using Twisted reactor {0}'.format(reactor.__class__))  # @UndefinedVariable

        alarms = Alarms(args.save)

        serialProtocol = SerialProtocol(args.mpd, alarms)

        # Create embedded web server to manage the alarms.
        if args.web:
            agent = Agent(reactor)
            webInterface = WebInterface(args.web, agent, alarms, serialProtocol)
            serialProtocol.setWebInterface(webInterface)

        if args.port:
            print('About to open serial port {0} [{1} baud] ..'.format(args.port, 9600))
            try:
                SerialPort(serialProtocol, args.port, reactor, baudrate=9600)
            except Exception as e:
                print('Could not open serial port: {0}'.format(e))
                traceback.print_exc()
                return 1

        # start the component and the Twisted reactor ..
        reactor.run()  # @UndefinedVariable

        return 0
    except KeyboardInterrupt:
        return 0


if __name__ == '__main__':
    if DEBUG:
        sys.argv.append('-h')
        sys.argv.append('-v')
        sys.argv.append('-r')
    if TESTRUN:
        import doctest
        doctest.testmod()
    if PROFILE:
        import cProfile
        import pstats
        profile_filename = 'alarmclock_profile.txt'
        cProfile.run('main()', profile_filename)
        statsfile = open('profile_stats.txt', 'wb')
        p = pstats.Stats(profile_filename, stream=statsfile)
        stats = p.strip_dirs().sort_stats('cumulative')
        stats.print_stats()
        statsfile.close()
        sys.exit(0)
    sys.exit(main())
