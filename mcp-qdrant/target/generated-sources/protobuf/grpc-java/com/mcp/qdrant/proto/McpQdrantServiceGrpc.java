package com.mcp.qdrant.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.59.0)",
    comments = "Source: mcp_contracts.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class McpQdrantServiceGrpc {

  private McpQdrantServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.vpms.mcp.qdrant.McpQdrantService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.HybridSearchRequest,
      com.mcp.qdrant.proto.HybridSearchResponse> getHybridSearchMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "HybridSearch",
      requestType = com.mcp.qdrant.proto.HybridSearchRequest.class,
      responseType = com.mcp.qdrant.proto.HybridSearchResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.HybridSearchRequest,
      com.mcp.qdrant.proto.HybridSearchResponse> getHybridSearchMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.HybridSearchRequest, com.mcp.qdrant.proto.HybridSearchResponse> getHybridSearchMethod;
    if ((getHybridSearchMethod = McpQdrantServiceGrpc.getHybridSearchMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getHybridSearchMethod = McpQdrantServiceGrpc.getHybridSearchMethod) == null) {
          McpQdrantServiceGrpc.getHybridSearchMethod = getHybridSearchMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.HybridSearchRequest, com.mcp.qdrant.proto.HybridSearchResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "HybridSearch"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.HybridSearchRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.HybridSearchResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("HybridSearch"))
              .build();
        }
      }
    }
    return getHybridSearchMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.IngestDocumentRequest,
      com.mcp.qdrant.proto.IngestDocumentResponse> getIngestDocumentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "IngestDocument",
      requestType = com.mcp.qdrant.proto.IngestDocumentRequest.class,
      responseType = com.mcp.qdrant.proto.IngestDocumentResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.IngestDocumentRequest,
      com.mcp.qdrant.proto.IngestDocumentResponse> getIngestDocumentMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.IngestDocumentRequest, com.mcp.qdrant.proto.IngestDocumentResponse> getIngestDocumentMethod;
    if ((getIngestDocumentMethod = McpQdrantServiceGrpc.getIngestDocumentMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getIngestDocumentMethod = McpQdrantServiceGrpc.getIngestDocumentMethod) == null) {
          McpQdrantServiceGrpc.getIngestDocumentMethod = getIngestDocumentMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.IngestDocumentRequest, com.mcp.qdrant.proto.IngestDocumentResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "IngestDocument"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.IngestDocumentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.IngestDocumentResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("IngestDocument"))
              .build();
        }
      }
    }
    return getIngestDocumentMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.CreateCollectionRequest,
      com.mcp.qdrant.proto.CreateCollectionResponse> getCreateCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "CreateCollection",
      requestType = com.mcp.qdrant.proto.CreateCollectionRequest.class,
      responseType = com.mcp.qdrant.proto.CreateCollectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.CreateCollectionRequest,
      com.mcp.qdrant.proto.CreateCollectionResponse> getCreateCollectionMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.CreateCollectionRequest, com.mcp.qdrant.proto.CreateCollectionResponse> getCreateCollectionMethod;
    if ((getCreateCollectionMethod = McpQdrantServiceGrpc.getCreateCollectionMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getCreateCollectionMethod = McpQdrantServiceGrpc.getCreateCollectionMethod) == null) {
          McpQdrantServiceGrpc.getCreateCollectionMethod = getCreateCollectionMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.CreateCollectionRequest, com.mcp.qdrant.proto.CreateCollectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreateCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.CreateCollectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.CreateCollectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("CreateCollection"))
              .build();
        }
      }
    }
    return getCreateCollectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.DeleteCollectionRequest,
      com.mcp.qdrant.proto.DeleteCollectionResponse> getDeleteCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeleteCollection",
      requestType = com.mcp.qdrant.proto.DeleteCollectionRequest.class,
      responseType = com.mcp.qdrant.proto.DeleteCollectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.DeleteCollectionRequest,
      com.mcp.qdrant.proto.DeleteCollectionResponse> getDeleteCollectionMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.DeleteCollectionRequest, com.mcp.qdrant.proto.DeleteCollectionResponse> getDeleteCollectionMethod;
    if ((getDeleteCollectionMethod = McpQdrantServiceGrpc.getDeleteCollectionMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getDeleteCollectionMethod = McpQdrantServiceGrpc.getDeleteCollectionMethod) == null) {
          McpQdrantServiceGrpc.getDeleteCollectionMethod = getDeleteCollectionMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.DeleteCollectionRequest, com.mcp.qdrant.proto.DeleteCollectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeleteCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.DeleteCollectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.DeleteCollectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("DeleteCollection"))
              .build();
        }
      }
    }
    return getDeleteCollectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.ListCollectionsRequest,
      com.mcp.qdrant.proto.ListCollectionsResponse> getListCollectionsMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ListCollections",
      requestType = com.mcp.qdrant.proto.ListCollectionsRequest.class,
      responseType = com.mcp.qdrant.proto.ListCollectionsResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.ListCollectionsRequest,
      com.mcp.qdrant.proto.ListCollectionsResponse> getListCollectionsMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.ListCollectionsRequest, com.mcp.qdrant.proto.ListCollectionsResponse> getListCollectionsMethod;
    if ((getListCollectionsMethod = McpQdrantServiceGrpc.getListCollectionsMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getListCollectionsMethod = McpQdrantServiceGrpc.getListCollectionsMethod) == null) {
          McpQdrantServiceGrpc.getListCollectionsMethod = getListCollectionsMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.ListCollectionsRequest, com.mcp.qdrant.proto.ListCollectionsResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ListCollections"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.ListCollectionsRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.ListCollectionsResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("ListCollections"))
              .build();
        }
      }
    }
    return getListCollectionsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.GetCollectionInfoRequest,
      com.mcp.qdrant.proto.GetCollectionInfoResponse> getGetCollectionInfoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetCollectionInfo",
      requestType = com.mcp.qdrant.proto.GetCollectionInfoRequest.class,
      responseType = com.mcp.qdrant.proto.GetCollectionInfoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.GetCollectionInfoRequest,
      com.mcp.qdrant.proto.GetCollectionInfoResponse> getGetCollectionInfoMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.GetCollectionInfoRequest, com.mcp.qdrant.proto.GetCollectionInfoResponse> getGetCollectionInfoMethod;
    if ((getGetCollectionInfoMethod = McpQdrantServiceGrpc.getGetCollectionInfoMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getGetCollectionInfoMethod = McpQdrantServiceGrpc.getGetCollectionInfoMethod) == null) {
          McpQdrantServiceGrpc.getGetCollectionInfoMethod = getGetCollectionInfoMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.GetCollectionInfoRequest, com.mcp.qdrant.proto.GetCollectionInfoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetCollectionInfo"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.GetCollectionInfoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.GetCollectionInfoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("GetCollectionInfo"))
              .build();
        }
      }
    }
    return getGetCollectionInfoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.BackupCollectionRequest,
      com.mcp.qdrant.proto.BackupCollectionResponse> getBackupCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "BackupCollection",
      requestType = com.mcp.qdrant.proto.BackupCollectionRequest.class,
      responseType = com.mcp.qdrant.proto.BackupCollectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.BackupCollectionRequest,
      com.mcp.qdrant.proto.BackupCollectionResponse> getBackupCollectionMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.BackupCollectionRequest, com.mcp.qdrant.proto.BackupCollectionResponse> getBackupCollectionMethod;
    if ((getBackupCollectionMethod = McpQdrantServiceGrpc.getBackupCollectionMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getBackupCollectionMethod = McpQdrantServiceGrpc.getBackupCollectionMethod) == null) {
          McpQdrantServiceGrpc.getBackupCollectionMethod = getBackupCollectionMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.BackupCollectionRequest, com.mcp.qdrant.proto.BackupCollectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "BackupCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.BackupCollectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.BackupCollectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("BackupCollection"))
              .build();
        }
      }
    }
    return getBackupCollectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.mcp.qdrant.proto.RestoreCollectionRequest,
      com.mcp.qdrant.proto.RestoreCollectionResponse> getRestoreCollectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "RestoreCollection",
      requestType = com.mcp.qdrant.proto.RestoreCollectionRequest.class,
      responseType = com.mcp.qdrant.proto.RestoreCollectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.mcp.qdrant.proto.RestoreCollectionRequest,
      com.mcp.qdrant.proto.RestoreCollectionResponse> getRestoreCollectionMethod() {
    io.grpc.MethodDescriptor<com.mcp.qdrant.proto.RestoreCollectionRequest, com.mcp.qdrant.proto.RestoreCollectionResponse> getRestoreCollectionMethod;
    if ((getRestoreCollectionMethod = McpQdrantServiceGrpc.getRestoreCollectionMethod) == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        if ((getRestoreCollectionMethod = McpQdrantServiceGrpc.getRestoreCollectionMethod) == null) {
          McpQdrantServiceGrpc.getRestoreCollectionMethod = getRestoreCollectionMethod =
              io.grpc.MethodDescriptor.<com.mcp.qdrant.proto.RestoreCollectionRequest, com.mcp.qdrant.proto.RestoreCollectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RestoreCollection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.RestoreCollectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.mcp.qdrant.proto.RestoreCollectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new McpQdrantServiceMethodDescriptorSupplier("RestoreCollection"))
              .build();
        }
      }
    }
    return getRestoreCollectionMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static McpQdrantServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceStub>() {
        @java.lang.Override
        public McpQdrantServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new McpQdrantServiceStub(channel, callOptions);
        }
      };
    return McpQdrantServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static McpQdrantServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceBlockingStub>() {
        @java.lang.Override
        public McpQdrantServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new McpQdrantServiceBlockingStub(channel, callOptions);
        }
      };
    return McpQdrantServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static McpQdrantServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<McpQdrantServiceFutureStub>() {
        @java.lang.Override
        public McpQdrantServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new McpQdrantServiceFutureStub(channel, callOptions);
        }
      };
    return McpQdrantServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void hybridSearch(com.mcp.qdrant.proto.HybridSearchRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.HybridSearchResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHybridSearchMethod(), responseObserver);
    }

    /**
     */
    default void ingestDocument(com.mcp.qdrant.proto.IngestDocumentRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.IngestDocumentResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getIngestDocumentMethod(), responseObserver);
    }

    /**
     */
    default void createCollection(com.mcp.qdrant.proto.CreateCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.CreateCollectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getCreateCollectionMethod(), responseObserver);
    }

    /**
     */
    default void deleteCollection(com.mcp.qdrant.proto.DeleteCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.DeleteCollectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getDeleteCollectionMethod(), responseObserver);
    }

    /**
     */
    default void listCollections(com.mcp.qdrant.proto.ListCollectionsRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.ListCollectionsResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getListCollectionsMethod(), responseObserver);
    }

    /**
     */
    default void getCollectionInfo(com.mcp.qdrant.proto.GetCollectionInfoRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getGetCollectionInfoMethod(), responseObserver);
    }

    /**
     */
    default void backupCollection(com.mcp.qdrant.proto.BackupCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.BackupCollectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBackupCollectionMethod(), responseObserver);
    }

    /**
     */
    default void restoreCollection(com.mcp.qdrant.proto.RestoreCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.RestoreCollectionResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRestoreCollectionMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service McpQdrantService.
   */
  public static abstract class McpQdrantServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return McpQdrantServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service McpQdrantService.
   */
  public static final class McpQdrantServiceStub
      extends io.grpc.stub.AbstractAsyncStub<McpQdrantServiceStub> {
    private McpQdrantServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected McpQdrantServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new McpQdrantServiceStub(channel, callOptions);
    }

    /**
     */
    public void hybridSearch(com.mcp.qdrant.proto.HybridSearchRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.HybridSearchResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHybridSearchMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void ingestDocument(com.mcp.qdrant.proto.IngestDocumentRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.IngestDocumentResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getIngestDocumentMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void createCollection(com.mcp.qdrant.proto.CreateCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.CreateCollectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getCreateCollectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deleteCollection(com.mcp.qdrant.proto.DeleteCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.DeleteCollectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getDeleteCollectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void listCollections(com.mcp.qdrant.proto.ListCollectionsRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.ListCollectionsResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getListCollectionsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getCollectionInfo(com.mcp.qdrant.proto.GetCollectionInfoRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.GetCollectionInfoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getGetCollectionInfoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void backupCollection(com.mcp.qdrant.proto.BackupCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.BackupCollectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBackupCollectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void restoreCollection(com.mcp.qdrant.proto.RestoreCollectionRequest request,
        io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.RestoreCollectionResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRestoreCollectionMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service McpQdrantService.
   */
  public static final class McpQdrantServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<McpQdrantServiceBlockingStub> {
    private McpQdrantServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected McpQdrantServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new McpQdrantServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.mcp.qdrant.proto.HybridSearchResponse hybridSearch(com.mcp.qdrant.proto.HybridSearchRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHybridSearchMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.IngestDocumentResponse ingestDocument(com.mcp.qdrant.proto.IngestDocumentRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getIngestDocumentMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.CreateCollectionResponse createCollection(com.mcp.qdrant.proto.CreateCollectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getCreateCollectionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.DeleteCollectionResponse deleteCollection(com.mcp.qdrant.proto.DeleteCollectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getDeleteCollectionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.ListCollectionsResponse listCollections(com.mcp.qdrant.proto.ListCollectionsRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getListCollectionsMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.GetCollectionInfoResponse getCollectionInfo(com.mcp.qdrant.proto.GetCollectionInfoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getGetCollectionInfoMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.BackupCollectionResponse backupCollection(com.mcp.qdrant.proto.BackupCollectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBackupCollectionMethod(), getCallOptions(), request);
    }

    /**
     */
    public com.mcp.qdrant.proto.RestoreCollectionResponse restoreCollection(com.mcp.qdrant.proto.RestoreCollectionRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRestoreCollectionMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service McpQdrantService.
   */
  public static final class McpQdrantServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<McpQdrantServiceFutureStub> {
    private McpQdrantServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected McpQdrantServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new McpQdrantServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.HybridSearchResponse> hybridSearch(
        com.mcp.qdrant.proto.HybridSearchRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHybridSearchMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.IngestDocumentResponse> ingestDocument(
        com.mcp.qdrant.proto.IngestDocumentRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getIngestDocumentMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.CreateCollectionResponse> createCollection(
        com.mcp.qdrant.proto.CreateCollectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getCreateCollectionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.DeleteCollectionResponse> deleteCollection(
        com.mcp.qdrant.proto.DeleteCollectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getDeleteCollectionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.ListCollectionsResponse> listCollections(
        com.mcp.qdrant.proto.ListCollectionsRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getListCollectionsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.GetCollectionInfoResponse> getCollectionInfo(
        com.mcp.qdrant.proto.GetCollectionInfoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getGetCollectionInfoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.BackupCollectionResponse> backupCollection(
        com.mcp.qdrant.proto.BackupCollectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBackupCollectionMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.mcp.qdrant.proto.RestoreCollectionResponse> restoreCollection(
        com.mcp.qdrant.proto.RestoreCollectionRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRestoreCollectionMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_HYBRID_SEARCH = 0;
  private static final int METHODID_INGEST_DOCUMENT = 1;
  private static final int METHODID_CREATE_COLLECTION = 2;
  private static final int METHODID_DELETE_COLLECTION = 3;
  private static final int METHODID_LIST_COLLECTIONS = 4;
  private static final int METHODID_GET_COLLECTION_INFO = 5;
  private static final int METHODID_BACKUP_COLLECTION = 6;
  private static final int METHODID_RESTORE_COLLECTION = 7;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_HYBRID_SEARCH:
          serviceImpl.hybridSearch((com.mcp.qdrant.proto.HybridSearchRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.HybridSearchResponse>) responseObserver);
          break;
        case METHODID_INGEST_DOCUMENT:
          serviceImpl.ingestDocument((com.mcp.qdrant.proto.IngestDocumentRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.IngestDocumentResponse>) responseObserver);
          break;
        case METHODID_CREATE_COLLECTION:
          serviceImpl.createCollection((com.mcp.qdrant.proto.CreateCollectionRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.CreateCollectionResponse>) responseObserver);
          break;
        case METHODID_DELETE_COLLECTION:
          serviceImpl.deleteCollection((com.mcp.qdrant.proto.DeleteCollectionRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.DeleteCollectionResponse>) responseObserver);
          break;
        case METHODID_LIST_COLLECTIONS:
          serviceImpl.listCollections((com.mcp.qdrant.proto.ListCollectionsRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.ListCollectionsResponse>) responseObserver);
          break;
        case METHODID_GET_COLLECTION_INFO:
          serviceImpl.getCollectionInfo((com.mcp.qdrant.proto.GetCollectionInfoRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.GetCollectionInfoResponse>) responseObserver);
          break;
        case METHODID_BACKUP_COLLECTION:
          serviceImpl.backupCollection((com.mcp.qdrant.proto.BackupCollectionRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.BackupCollectionResponse>) responseObserver);
          break;
        case METHODID_RESTORE_COLLECTION:
          serviceImpl.restoreCollection((com.mcp.qdrant.proto.RestoreCollectionRequest) request,
              (io.grpc.stub.StreamObserver<com.mcp.qdrant.proto.RestoreCollectionResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getHybridSearchMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.HybridSearchRequest,
              com.mcp.qdrant.proto.HybridSearchResponse>(
                service, METHODID_HYBRID_SEARCH)))
        .addMethod(
          getIngestDocumentMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.IngestDocumentRequest,
              com.mcp.qdrant.proto.IngestDocumentResponse>(
                service, METHODID_INGEST_DOCUMENT)))
        .addMethod(
          getCreateCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.CreateCollectionRequest,
              com.mcp.qdrant.proto.CreateCollectionResponse>(
                service, METHODID_CREATE_COLLECTION)))
        .addMethod(
          getDeleteCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.DeleteCollectionRequest,
              com.mcp.qdrant.proto.DeleteCollectionResponse>(
                service, METHODID_DELETE_COLLECTION)))
        .addMethod(
          getListCollectionsMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.ListCollectionsRequest,
              com.mcp.qdrant.proto.ListCollectionsResponse>(
                service, METHODID_LIST_COLLECTIONS)))
        .addMethod(
          getGetCollectionInfoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.GetCollectionInfoRequest,
              com.mcp.qdrant.proto.GetCollectionInfoResponse>(
                service, METHODID_GET_COLLECTION_INFO)))
        .addMethod(
          getBackupCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.BackupCollectionRequest,
              com.mcp.qdrant.proto.BackupCollectionResponse>(
                service, METHODID_BACKUP_COLLECTION)))
        .addMethod(
          getRestoreCollectionMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.mcp.qdrant.proto.RestoreCollectionRequest,
              com.mcp.qdrant.proto.RestoreCollectionResponse>(
                service, METHODID_RESTORE_COLLECTION)))
        .build();
  }

  private static abstract class McpQdrantServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    McpQdrantServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.mcp.qdrant.proto.McpContracts.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("McpQdrantService");
    }
  }

  private static final class McpQdrantServiceFileDescriptorSupplier
      extends McpQdrantServiceBaseDescriptorSupplier {
    McpQdrantServiceFileDescriptorSupplier() {}
  }

  private static final class McpQdrantServiceMethodDescriptorSupplier
      extends McpQdrantServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    McpQdrantServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (McpQdrantServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new McpQdrantServiceFileDescriptorSupplier())
              .addMethod(getHybridSearchMethod())
              .addMethod(getIngestDocumentMethod())
              .addMethod(getCreateCollectionMethod())
              .addMethod(getDeleteCollectionMethod())
              .addMethod(getListCollectionsMethod())
              .addMethod(getGetCollectionInfoMethod())
              .addMethod(getBackupCollectionMethod())
              .addMethod(getRestoreCollectionMethod())
              .build();
        }
      }
    }
    return result;
  }
}
