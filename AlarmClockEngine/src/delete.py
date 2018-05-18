'''JSON data endpoint for getting and setting alarms. 

'''

import json
import logging
import webapp2

from google.appengine.ext import ndb

import models
import utils


class JsonEndpoint(webapp2.RequestHandler):

    def post(self):
        user = utils.getLoggedInUser(self.request.headers)
        if not user:
            self.abort(401)

        content = self.request.body.decode('utf-8')
        logging.info('Received content: ' + content)
        inputData = json.loads(content)
        logging.info('Received JSON: ' + str(inputData))

        clock_key = ndb.Key(urlsafe=inputData['clock_key'])
        clock = clock_key.get()
        userClock = models.UserClock.get_by_clock(user, clock_key)
        alarms = models.Alarm.query_by_clock(clock_key).fetch(100)

        to_delete = [alarm.key for alarm in alarms]
        if userClock:
            to_delete.append(userClock.key)
        if clock:
            to_delete.append(clock.key)
        ndb.delete_multi(to_delete)
        
        data = {}

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/delete', JsonEndpoint),
], debug=True)
