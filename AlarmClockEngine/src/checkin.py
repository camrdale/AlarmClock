'''JSON data endpoint for getting and setting alarms. 

'''

import datetime
import json
import logging
import webapp2

from google.appengine.ext import ndb

import models


class JsonEndpoint(webapp2.RequestHandler):

    def post(self):
        content = self.request.body.decode('utf-8')
        logging.info('Received content: ' + content)
        inputData = json.loads(content)
        logging.info('Received JSON: ' + str(inputData))

        clock_key = ndb.Key(urlsafe=inputData['clock_key'])
        clock = clock_key.get()
        if not clock:
            self.abort(404)
        clock.last_checkin = datetime.datetime.utcnow()
        clock.put()
        
        data = {}
        if clock.claimed:
            data['claimed'] = True
            data['revision'] = clock.revision
            alarms = models.Alarm.query_by_clock(clock_key).fetch(100)
            data['alarms'] = [
                {'crontab': alarm.crontab, 'buzzer': alarm.buzzer}
                for alarm in alarms]
            for alarm in alarms:
                if alarm.clock_fetch_time is None:
                    alarm.clock_fetch_time = datetime.datetime.utcnow()
                    alarm.put()
        else:
            data['claimed'] = False

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/checkin', JsonEndpoint),
], debug=True)
