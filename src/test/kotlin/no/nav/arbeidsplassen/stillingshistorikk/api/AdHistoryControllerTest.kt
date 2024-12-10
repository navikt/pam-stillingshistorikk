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
class AdHistoryControllerTest : TestRunningApplication() {

    @Test
    fun `Skal hente stillingshistorikk`() {
        val request = HttpRequest.newBuilder()
            .uri(URI("$lokalBaseUrl/api/v1/ads/history/uuid?year=2024"))
            .GET()
            .build()
        val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.code)
    }

    @Test
    fun `Skal hente stillingshistorikk uten år og returnere 400`() {
        val request = HttpRequest.newBuilder()
            .uri(URI("$lokalBaseUrl/api/v1/ads/history/uuid"))
            .GET()
            .build()
        val response = HttpClient.newBuilder().build().send(request, BodyHandlers.ofString())
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.code)
    }
}