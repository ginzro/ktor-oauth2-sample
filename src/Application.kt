package com.example

import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.client.*
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.html.*
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.response.respondRedirect
import io.ktor.routing.*
import kotlinx.html.*
import java.net.URL

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)


@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Authentication) {
        oauth(COGNITO) {
            client = HttpClient()
            providerLookup = {
                val domain = getEnv("cognito.domain")
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "cognito",
                    authorizeUrl = "$domain/oauth2/authorize",
                    accessTokenUrl = "$domain/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = getEnv("cognito.clientId"),
                    clientSecret = getEnv("cognito.clientSecret")
                )
            }
            urlProvider = { "http://localhost:8080/login" }
        }
    }
    routing {
        authenticate(COGNITO) {
            get("/login") {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()
                if (principal == null) {
                    call.respondRedirect("http://localhost:8080/")
                } else {
                    val domain = getEnv("cognito.domain")
                    val client = HttpClient()
                    val result = client.get<String>("$domain/oauth2/userInfo") {
                        header("Authorization", "Bearer ${principal.accessToken}")
                    }
                    call.respondHtml {
                        body {
                            h1 { +"you are login." }
                            a { +result }
                        }
                    }
                }
            }
        }

        get("/") {
            call.respondHtml {
                head {
                    title { +"Login with" }
                }
                body {
                    h1 { +"Login with:" }
                    a(href = "/login") { +"cognito" }
                }
            }
        }
    }
}

const val COGNITO = "cognito"
private fun Application.getEnv(name: String): String {
    return environment.config.property(name).getString()
}
