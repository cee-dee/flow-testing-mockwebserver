package com.example.flowtesting.utils

import com.google.common.truth.Truth.assertThat

fun Result<*>.assertHasException(exceptionClass: Class<*>, messageContains: String) {
    assertThat(isFailure).isTrue()
    assertThat(exceptionOrNull()).isInstanceOf(exceptionClass)
    assertThat(exceptionOrNull()).hasMessageThat().contains(messageContains)
}
