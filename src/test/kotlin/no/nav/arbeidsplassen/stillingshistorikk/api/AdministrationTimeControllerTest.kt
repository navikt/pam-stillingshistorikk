package no.nav.arbeidsplassen.stillingshistorikk.api

import io.javalin.http.HttpStatus
import no.nav.arbeidsplassen.stillingshistorikk.app.test.TestRunningApplication
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class AdministrationTimeControllerTest : TestRunningApplication() {

    @Test
    fun `Skal hente behandlingstid`() {
        val request = HttpRequest.newBuilder()
            .uri(URI("$lokalBaseUrl/api/v1/admin/report/behandlingstid.csv"))
            .GET()
            .build()
        val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.code)
    }
}