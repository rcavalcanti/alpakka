/*
 * Copyright (C) 2016-2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.stream.alpakka.s3.impl

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.headers.ByteRange
import akka.stream.alpakka.s3.MemoryBufferType
import akka.stream.alpakka.s3.S3Settings
import akka.stream.alpakka.s3.auth.AWSCredentials
import akka.testkit.TestKit
import org.scalatest.FlatSpecLike
import org.scalatest.Matchers
import org.scalatest.PrivateMethodTester

class S3StreamSpec(_system: ActorSystem)
    extends TestKit(_system)
    with FlatSpecLike
    with Matchers
    with PrivateMethodTester {

  import HttpRequests._

  def this() = this(ActorSystem("S3StreamSpec"))
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system).withDebugLogging(true))

  "Non-ranged downloads" should "have one (host) header" in {

    val requestHeaders = PrivateMethod[HttpRequest]('requestHeaders)
    val credentials = AWSCredentials(accessKeyId = "test-Id", secretAccessKey = "test-key")
    val location = S3Location("test-bucket", "test-key")

    implicit val settings = new S3Settings(MemoryBufferType, "", None, credentials, "us-east-1", false)

    val s3stream = new S3Stream(settings)
    val result: HttpRequest = s3stream invokePrivate requestHeaders(getDownloadRequest(location), None)
    result.headers.size shouldBe 1
    result.headers.seq.exists(_.lowercaseName() == "host")
  }

  "Ranged downloads" should "have two (host, range) headers" in {

    val requestHeaders = PrivateMethod[HttpRequest]('requestHeaders)
    val credentials = AWSCredentials(accessKeyId = "test-Id", secretAccessKey = "test-key")
    val location = S3Location("test-bucket", "test-key")
    val range = ByteRange(1, 4)

    implicit val settings = new S3Settings(MemoryBufferType, "", None, credentials, "us-east-1", false)

    val s3stream = new S3Stream(settings)
    val result: HttpRequest = s3stream invokePrivate requestHeaders(getDownloadRequest(location), Some(range))
    result.headers.size shouldBe 2
    result.headers.seq.exists(_.lowercaseName() == "host")
    result.headers.seq.exists(_.lowercaseName() == "range")

  }
}
