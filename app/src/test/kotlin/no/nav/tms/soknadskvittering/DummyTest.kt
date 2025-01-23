package no.nav.tms.soknadskvittering

import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tms.soknadskvittering.setup.LocalPostgresDatabase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DummyTest {
    private val database = LocalPostgresDatabase.cleanDb()
    private val testFnr = "12345678910"


    @Test
    fun test() {
        database.single {
            queryOf(
                "select count(*) as antall from soknad"
            ).map { it.int("antall") }.asSingle
        } shouldBe 0
    }
}
