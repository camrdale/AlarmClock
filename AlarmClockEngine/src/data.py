'''JSON data endpoint for getting and setting alarms. 

'''

import datetime
import json
import logging
import pytz
import uuid
import webapp2

from google.appengine.ext import ndb

import models
import utils

NUM_ALARMS_DISPLAY = 10


class JsonEndpoint(webapp2.RequestHandler):

    def post(self):
        user = utils.getLoggedInUser(self.request.headers)
        if not user:
            self.abort(401)
        
        content = self.request.body.decode('utf-8')
        logging.info('Received content: ' + content)
        inputData = json.loads(content)
        logging.info('Received JSON: ' + str(inputData))

        for clock in inputData.get('clocks', []):
            clock_key = ndb.Key(urlsafe=clock['clock_key'])
            clockModel = clock_key.get()
            if not clockModel:
                self.abort(404)

            old_alarms = models.Alarm.query_by_clock(clock_key).fetch(100)
            ndb.delete_multi(alarm.key for alarm in old_alarms)

            for alarm in clock.get('new_alarms', []):
                new_alarm = models.Alarm(
                    crontab=alarm['crontab'],
                    buzzer=alarm['buzzer'],
                    parent=clock_key)
                new_alarm.put()
            
            clockModel.revision = str(uuid.uuid4())
            clockModel.put()
            
        num_alarms_display = inputData.get('num_alarms_display', NUM_ALARMS_DISPLAY)
        
        data = {'clocks': []}
        timezone = pytz.timezone("America/Los_Angeles")

        userClocks = models.UserClock.query_by_user(user).fetch(100)
        clocks = ndb.get_multi(userClock.clock for userClock in userClocks)
        for userClock, clock in zip(userClocks, clocks):
            timezone = pytz.timezone(clock.time_zone)
            alarms = models.Alarm.query_by_clock(clock.key).fetch(100)
            data['clocks'].append({
                'clock_key': clock.key.urlsafe(),
                'name': userClock.name,
                'time_zone': clock.time_zone,
                'alarms': [
                    {'crontab': alarm.crontab, 'buzzer': alarm.buzzer}
                    for alarm in alarms]})
        
        now = pytz.utc.localize(datetime.datetime.utcnow()).astimezone(timezone)
        data['now'] = now.strftime('%I:%M:%S %p %A, %B %d, %Y')
        data['num_alarms_display'] = num_alarms_display
        data['next_alarms'] = ['08:30:00 AM Monday, April 30, 2018']
        # TODO: retrieve saved alarms
        #    alarm.strftime('%I:%M:%S %p %A, %B %d, %Y')
        #    for alarm in self._alarms.next_alarms(num_alarms_display, now=now)]

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/data', JsonEndpoint),
], debug=True)
