package com.neo.sk.gypsy.bot

import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}
import org.seekloud.esheepapi.pb.api._
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc
import org.seekloud.esheepapi.pb.service.EsheepAgentGrpc.EsheepAgent

import scala.concurrent.{ExecutionContext, Future}


class GrpcServer {

  private var server: Option[Server] = None

  def start(service: ServerServiceDefinition, executionContext: ExecutionContext, port: Int): Boolean = {
    server match {
      case None =>
        val s = ServerBuilder.forPort(port).addService(service).build
        s.start()
        server = Some(s)
        true
      case _ => false
    }
  }

  def stop(): Unit = server.foreach(_.shutdown())

  def blockUntilShutdown(): Unit = server.foreach(_.awaitTermination())

}














