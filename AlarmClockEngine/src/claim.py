'''JSON data endpoint for getting and setting alarms. 

'''

import json
import logging
import uuid
import webapp2

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

        verification_number = inputData['verification_number']
        
        clock = models.Clock.get_by_verification_number(verification_number)
        
        if not clock:
            self.abort(404)
        
        clock.time_zone = 'America/Los_Angeles'
        clock.claimed = True
        clock.verification_number = None
        clock.revision = str(uuid.uuid4())
        clock_key = clock.put()

        userClock = models.UserClock(name='Placeholder', clock=clock_key, parent=user.key)
        userClock.put()
        
        data = {
            'clock_key': clock_key.urlsafe(),
            'name': userClock.name,
            'time_zone': clock.time_zone}

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/claim', JsonEndpoint),
], debug=True)
