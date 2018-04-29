'''
Created on Apr 26, 2018

@author: camrdale
'''

import datetime

from google.appengine.ext import ndb


class Clock(ndb.Model):
    claimed = ndb.BooleanProperty()
    verification_number = ndb.StringProperty()
    last_checkin = ndb.DateTimeProperty()
    time_zone = ndb.StringProperty()
    creation_time = ndb.DateTimeProperty(auto_now_add=True)
    revision = ndb.StringProperty()

    @classmethod
    def get_by_verification_number(cls, number):
        return cls.query(cls.claimed == False, cls.verification_number == number).get()

    @classmethod
    def stale_unverified(cls):
        max_age = datetime.datetime.utcnow() - datetime.timedelta(minutes=10)
        return cls.query(cls.claimed == False, cls.creation_time < max_age)


class Alarm(ndb.Model):
    crontab = ndb.StringProperty()
    buzzer = ndb.BooleanProperty()

    @classmethod
    def query_by_clock(cls, clock_key):
        return cls.query(ancestor=clock_key)


class User(ndb.Model):
    @classmethod
    def get(cls, current_user):
        return cls.get_or_insert(current_user.user_id())


class UserClock(ndb.Model):
    name = ndb.StringProperty()
    clock = ndb.KeyProperty(kind=Clock)

    @classmethod
    def query_by_user(cls, user):
        return cls.query(ancestor=user.key)

    @classmethod
    def get_by_clock(cls, user, clock_key):
        return cls.query_by_user(user).filter(UserClock.clock == clock_key).get()
