package io.examples.infrastructure;

import io.examples.order.domain.Order;
import io.examples.order.domain.OrderEntity;
import io.examples.order.domain.OrderState;
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.vlingo.actors.World;
import io.vlingo.http.resource.Server;
import io.vlingo.lattice.model.object.ObjectTypeRegistry;
import io.vlingo.lattice.model.object.ObjectTypeRegistry.Info;
import io.vlingo.symbio.store.MapQueryExpression;
import io.vlingo.symbio.store.object.ObjectStore;
import io.vlingo.symbio.store.object.StateObjectMapper;
import io.vlingo.symbio.store.object.inmemory.InMemoryObjectStoreActor;
import io.vlingo.xoom.VlingoServer;

import java.util.Arrays;

/**
 * The {@code OrderApplication} is a microservice that implements features in the {@link OrderEntity} context.
 */
public class Bootstrap {

    private static final String MESSAGING_URI = "MESSAGING_URI";
    private static final String DATABASE_URL = "ORDER_SERVICE_DATABASE_URL";

    private static Bootstrap instance;
    private Server server;
    private World world;

    public static void main(String[] args) {
        Bootstrap.boot();
    }

    public static void boot() {
        Bootstrap.instance();
    }

    public static Bootstrap instance() {
        if (instance == null) {
            final ApplicationContext applicationContext = Bootstrap.run();

            instance = new Bootstrap(applicationContext.getBean(VlingoServer.class));
        }
        return instance;
    }

    private static ApplicationContext run() {
        return Micronaut.build(new String[]{})
                .environmentVariableIncludes(DATABASE_URL)
                .environmentVariableIncludes(MESSAGING_URI)
                .run(Bootstrap.class);
    }

    private Bootstrap(final VlingoServer vlingoServer) {
        this.server = vlingoServer.getServer();
        this.world = vlingoServer.getVlingoScene().getWorld();

        final MapQueryExpression objectQuery =
                MapQueryExpression.using(Order.class, "find", MapQueryExpression.map("id", "id"));

        final ObjectStore objectStore =
                world.stage().actorFor(ObjectStore.class, InMemoryObjectStoreActor.class, Arrays.asList(new MockDispatcher()));

        final StateObjectMapper stateObjectMapper =
                StateObjectMapper.with(Order.class, new Object(), new Object());

        final Info<OrderState> info =
                new Info(objectStore, OrderState.class,
                        "ObjectStore", objectQuery, stateObjectMapper);

        new ObjectTypeRegistry(world).register(info);

        OrderQueryProvider.using(world.stage(), objectStore);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stopAndCleanup();
        }));
    }

    public void stopAndCleanup() {
        instance = null;
        world.terminate();
        server.stop();
        OrderQueryProvider.reset();
        pause();
    }

    private void pause() {
        try {
            Thread.sleep(1000L);
        } catch (Exception e) {
            // ignore
        }
    }

}
