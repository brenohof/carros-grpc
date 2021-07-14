package br.com.zup.carros

import br.com.zup.CarrosGrpcServiceGrpc
import br.com.zup.CarrosRequest
import br.com.zup.CarrosResponse
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.micronaut.data.annotation.Repository
import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.ConstraintViolationException

@Singleton
class CarrosEndpoint(@Inject val repository:  CarroRepository) : CarrosGrpcServiceGrpc.CarrosGrpcServiceImplBase() {

    override fun adicionar(request: CarrosRequest, responseObserver: StreamObserver<CarrosResponse>) {

        if (repository.existsByPlaca(request.placa)) {
            responseObserver.onError(Status.ALREADY_EXISTS
                .withDescription("Carro com placa existente.")
                .asRuntimeException())
            return
        }

        val carro = Carro(
            modelo = request.modelo,
            placa = request.placa
        )

        try {
            repository.save(carro)
        } catch (e : ConstraintViolationException) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                .withDescription("dados de entrada inválidos.")
                .asRuntimeException())
            return
        }

        val response = CarrosResponse.newBuilder().setId(carro.id!!).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}