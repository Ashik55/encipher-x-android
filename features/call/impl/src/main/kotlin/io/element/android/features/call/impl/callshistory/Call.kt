/*
 * Copyright 2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.features.call.impl.callshistory

import android.os.Parcel
import android.os.Parcelable

/**
 * Model class representing a call in the call history.
 */
data class Call(
    val call_id: Long? = null,
    val call_type: String? = null,
    val caller_user_id: String? = null,
    val room_id: String? = null,
    val created_ts: String? = null,
    val ended_ts: String? = null,
    val caller_display_name: String? = null,
    val room_name: String? = null,
    val room_avatar: String? = null,
    val caller_avatar: String? = null,
    val is_caller: Boolean? = null,
    val receiver_user_ids: List<String>? = null,
    val receiver_display_names: Map<String, String>? = null,
    val receiver_avatars: Map<String, String>? = null,
    val isDm: Boolean = false
) : Parcelable {
    
    constructor(parcel: Parcel) : this(
        call_id = parcel.readValue(Long::class.java.classLoader) as? Long,
        call_type = parcel.readString(),
        caller_user_id = parcel.readString(),
        room_id = parcel.readString(),
        created_ts = parcel.readString(),
        ended_ts = parcel.readString(),
        caller_display_name = parcel.readString(),
        room_name = parcel.readString(),
        room_avatar = parcel.readString(),
        caller_avatar = parcel.readString(),
        is_caller = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        receiver_user_ids = parcel.createStringArrayList(),
        receiver_display_names = readStringMap(parcel),
        receiver_avatars = readStringMap(parcel),
        isDm = parcel.readBoolean()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeValue(call_id)
        parcel.writeString(call_type)
        parcel.writeString(caller_user_id)
        parcel.writeString(room_id)
        parcel.writeString(created_ts)
        parcel.writeString(ended_ts)
        parcel.writeString(caller_display_name)
        parcel.writeString(room_name)
        parcel.writeString(room_avatar)
        parcel.writeString(caller_avatar)
        parcel.writeValue(is_caller)
        parcel.writeStringList(receiver_user_ids)
        writeStringMap(parcel, receiver_display_names)
        writeStringMap(parcel, receiver_avatars)
        parcel.writeBoolean(isDm)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Call> {
        override fun createFromParcel(parcel: Parcel): Call {
            return Call(parcel)
        }

        override fun newArray(size: Int): Array<Call?> {
            return arrayOfNulls(size)
        }
        
        private fun readStringMap(parcel: Parcel): Map<String, String>? {
            val size = parcel.readInt()
            if (size < 0) return null
            
            val result = HashMap<String, String>(size)
            for (i in 0 until size) {
                val key = parcel.readString() ?: continue
                val value = parcel.readString() ?: continue
                result[key] = value
            }
            return result
        }
        
        private fun writeStringMap(parcel: Parcel, map: Map<String, String>?) {
            if (map == null) {
                parcel.writeInt(-1)
                return
            }
            
            parcel.writeInt(map.size)
            for ((key, value) in map) {
                parcel.writeString(key)
                parcel.writeString(value)
            }
        }
    }
}