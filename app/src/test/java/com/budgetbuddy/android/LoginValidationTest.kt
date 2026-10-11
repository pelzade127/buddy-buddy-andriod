package com.budgetbuddy.android

import com.budgetbuddy.android.data.LoginViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class LoginValidationTest {

    @Test fun blankInput_showsError_andDoesNotSucceed() {
        val vm = LoginViewModel()
        var succeeded = false
        vm.login(onSuccess = { succeeded = true })
        assertEquals("Please enter your email and password.", vm.errorMessage)
        assertFalse(succeeded)
    }

    @Test fun typing_clearsTheError() {
        val vm = LoginViewModel()
        vm.login(onSuccess = {})          // triggers the blank-field error
        vm.onEmailChange("test@test.com") // typing should clear it
        assertNull(vm.errorMessage)
    }
}