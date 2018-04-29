'''JSON data endpoint for getting and setting alarms. 

'''

import logging
import webapp2

from google.appengine.ext import ndb

import models


class CronEndpoint(webapp2.RequestHandler):

    def get(self):
        clocks = models.Clock.stale_unverified().fetch(100)
        logging.info('Found ' + str(len(clocks)) + ' stale clocks: ' + str(clocks))
        
        if not clocks:
            self.response.headers['Content-Type'] = 'text/plain'
            self.response.write('Nothing to delete'.encode('utf-8'))
            return
        
        ndb.delete_multi(clock.key for clock in clocks)

        logging.info('Deleted ' + str(len(clocks)) + ' stale clocks: ' + str(clocks))
        self.response.headers['Content-Type'] = 'text/plain'
        self.response.write(('Deleted ' + str(len(clocks)) + ' stale clocks').encode('utf-8'))


app = webapp2.WSGIApplication([
    ('/cleanup', CronEndpoint),
], debug=True)
