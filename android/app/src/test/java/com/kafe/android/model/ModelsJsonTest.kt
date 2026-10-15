package com.kafe.android.model

import com.google.gson.Gson
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsJsonTest {

    // Shape of a user as returned by the backend (e.g. inside a reservation)
    private fun userJson(banned: Boolean) = """
        {"id":1,"name":"Test Customer","phoneNumber":"012345678","email":null,
         "emailConfirmed":false,"noShowCount":3,"role":"CUSTOMER","cafeId":null,"banned":$banned}
    """.trimIndent()

    @Test
    fun user_readsBannedFlagFromBackend() {
        val user = Gson().fromJson(userJson(banned = true), User::class.java)
        assertTrue(user.isBanned)
    }

    @Test
    fun user_notBanned() {
        val user = Gson().fromJson(userJson(banned = false), User::class.java)
        assertFalse(user.isBanned)
    }
}
