package com.github.evis.highloadcup2017.model

import java.time.Instant

case class User(id: Int,
                email: String,
                firstName: String,
                lastName: String,
                gender: Gender,
                birthDate: Instant)
