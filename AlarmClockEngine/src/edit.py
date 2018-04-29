'''JSON data endpoint for getting and setting alarms. 

'''

import json
import logging
import webapp2

from google.appengine.api import users
from google.appengine.ext import ndb

import models


class JsonEndpoint(webapp2.RequestHandler):

    def post(self):
        user = models.User.get(users.get_current_user())
        content = self.request.body.decode('utf-8')
        logging.info('Received content: ' + content)
        inputData = json.loads(content)
        logging.info('Received JSON: ' + str(inputData))

        clock_key = ndb.Key(urlsafe=inputData['clock_key'])
        clock = clock_key.get()
        userClock = models.UserClock.get_by_clock(user, clock_key)
        
        if not clock or not userClock:
            self.abort(404)
            
        if 'name' in inputData:
            userClock.name = inputData['name']
            userClock.put()
        if 'time_zone' in inputData:
            clock.time_zone = inputData['time_zone']
            clock.put()
        
        data = {}

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/edit', JsonEndpoint),
], debug=True)
