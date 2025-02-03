package no.nav.tms.soknadskvittering.common

import io.kotest.matchers.shouldBe
import java.time.ZonedDateTime

infix fun ZonedDateTime.shouldBeSameTimeAs(other: ZonedDateTime) {
    toInstant().toEpochMilli() shouldBe other.toInstant().toEpochMilli()
}
