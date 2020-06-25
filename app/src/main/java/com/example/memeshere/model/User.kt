package com.example.memeshere.model

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(val uid: String, val username: String, val profileImg: String): Parcelable {
    constructor() : this("", "", "")
}