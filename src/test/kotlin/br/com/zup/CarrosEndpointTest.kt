package br.com.zup

import br.com.zup.carros.Carro
import br.com.zup.carros.CarroRepository
import io.grpc.ManagedChannel
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import io.micronaut.grpc.server.GrpcServerChannel
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.inject.Singleton

@MicronautTest(transactional = false)
class CarrosEndpointTest(val repository: CarroRepository, private val gRpcClient: CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub) {

    @BeforeEach
    internal fun setUp() {
        repository.deleteAll()
    }

    @Test
    internal fun `deve adicionar um novo carro`() {

        val response = gRpcClient.adicionar(CarrosRequest.newBuilder()
            .setModelo("Gol")
            .setPlaca("HPX-1234")
            .build())

        with(response) {
            assertNotNull(id)
            assertTrue(repository.existsById(id)) // efeito colateral
        }
    }

    @Test
    internal fun `nao deve adicionar novo carro quando placa ja existente`() {
        val existente = repository.save(Carro(modelo = "Palio", placa = "OIP-9876"))

        val error = assertThrows<StatusRuntimeException> {
            gRpcClient.adicionar(
                CarrosRequest.newBuilder()
                    .setModelo("Ferrari")
                    .setPlaca(existente.placa)
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.ALREADY_EXISTS.code, status.code)
            assertEquals("Carro com placa existente.", status.description)
        }
    }

    @Test
    internal fun `nao deve adicionar no carro quando dados de entrada forem invalidos`() {

        val error = assertThrows<StatusRuntimeException> {
            gRpcClient.adicionar(
                CarrosRequest.newBuilder()
                    .setModelo("")
                    .setPlaca("")
                    .build()
            )
        }

        with(error) {
            assertEquals(Status.INVALID_ARGUMENT.code, status.code)
            assertEquals("dados de entrada inválidos.", status.description)
        }
    }

    @Factory
    class Clients {

        @Singleton
        fun blockingStub(@GrpcChannel(GrpcServerChannel.NAME) channel: ManagedChannel): CarrosGrpcServiceGrpc.CarrosGrpcServiceBlockingStub? {
            return CarrosGrpcServiceGrpc.newBlockingStub(channel)
        }
    }

}