'''Utility functions. 

'''

import logging

import google.auth.transport.requests
import google.oauth2.id_token
import requests_toolbelt.adapters.appengine

import models

# Use the App Engine Requests adapter. This makes sure that Requests uses
# URLFetch.
requests_toolbelt.adapters.appengine.monkeypatch()
HTTP_REQUEST = google.auth.transport.requests.Request()


def getLoggedInUser(headers):
    id_token = headers['Authorization'].split(' ').pop()
    claims = google.oauth2.id_token.verify_firebase_token(
        id_token, HTTP_REQUEST)
    if not claims:
        return None

    # Some providers do not provide one of these so either can be used.
    username = claims.get('name', claims.get('email', 'Unknown'))
    logging.info('Logged in user ' + username + ': ' + str(claims))

    user = models.User.get(claims['sub'])
    return user
