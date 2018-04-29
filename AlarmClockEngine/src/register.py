'''JSON data endpoint for getting and setting alarms. 

'''

import json
import logging
import webapp2

import models


class JsonEndpoint(webapp2.RequestHandler):

    def post(self):
        content = self.request.body.decode('utf-8')
        logging.info('Received content: ' + content)
        inputData = json.loads(content)
        logging.info('Received JSON: ' + str(inputData))

        verification_number = inputData['verification_number']
        
        new_clock = models.Clock(claimed=False, verification_number=verification_number)
        clock_key = new_clock.put()
        
        data = {'clock_key': clock_key.urlsafe()}

        logging.info('Sending JSON: ' + str(data))
        content = json.dumps(data)
        logging.info('Sending content: ' + content)
        
        self.response.headers['Content-Type'] = 'application/json'
        self.response.write(content.encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/register', JsonEndpoint),
], debug=True)
