package br.com.officyna.seed;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class DatabaseSeederTest {

    private CustomerSeeder customerSeeder;
    private VehicleSeeder vehicleSeeder;
    private DatabaseSeeder databaseSeeder;

    @BeforeEach
    void setUp() {
        customerSeeder = mock(CustomerSeeder.class);
        vehicleSeeder = mock(VehicleSeeder.class);

        databaseSeeder = new DatabaseSeeder(
                customerSeeder,
                vehicleSeeder
        );
    }

    @Test
    void shouldExecuteAllSeeders() {
        databaseSeeder.run();

        verify(customerSeeder).seed();
        verify(vehicleSeeder).seed();

        verifyNoMoreInteractions(
                customerSeeder,
                vehicleSeeder
        );
    }
}