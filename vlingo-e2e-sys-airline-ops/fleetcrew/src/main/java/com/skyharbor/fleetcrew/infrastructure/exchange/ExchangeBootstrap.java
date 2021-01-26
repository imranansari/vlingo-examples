package com.skyharbor.fleetcrew.infrastructure.exchange;

import io.vlingo.actors.Stage;
import io.vlingo.xoom.actors.Settings;
import io.vlingo.lattice.exchange.Exchange;
import io.vlingo.xoom.exchange.ExchangeSettings;
import io.vlingo.lattice.exchange.rabbitmq.ExchangeFactory;
import io.vlingo.lattice.exchange.ConnectionSettings;
import io.vlingo.lattice.exchange.rabbitmq.Message;
import io.vlingo.lattice.exchange.rabbitmq.MessageSender;
import io.vlingo.lattice.exchange.Covey;
import io.vlingo.symbio.store.dispatch.Dispatcher;

import io.vlingo.lattice.model.IdentifiedDomainEvent;
import com.skyharbor.fleetcrew.infrastructure.AircraftData;

public class ExchangeBootstrap {

  private static ExchangeBootstrap instance;

  private final Dispatcher dispatcher;

  public static ExchangeBootstrap init(final Stage stage) {
    if(instance != null) {
      return instance;
    }

    ExchangeSettings.load(Settings.properties());

    final ConnectionSettings vgoAirportsSettings =
                ExchangeSettings.of("fleet-crew-exchange").mapToConnection();

    final Exchange fleetCrewExchange =
                ExchangeFactory.fanOutInstance(vgoAirportsSettings, "fleet-crew-exchange", true);

    fleetCrewExchange.register(Covey.of(
        new MessageSender(fleetCrewExchange.connection()),
        new AircraftExchangeReceivers.FlightLanded(stage),
        new AircraftConsumerAdapter("SkyHarborPHX:groundops:com.skyharbor.airtrafficcontrol:FlightLanded:1.0.0"),
        AircraftData.class,
        String.class,
        Message.class));

    fleetCrewExchange.register(Covey.of(
        new MessageSender(fleetCrewExchange.connection()),
        received -> {},
        new AircraftProducerAdapter(),
        IdentifiedDomainEvent.class,
        IdentifiedDomainEvent.class,
        Message.class));

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        fleetCrewExchange.close();

        System.out.println("\n");
        System.out.println("==================");
        System.out.println("Stopping exchange.");
        System.out.println("==================");
    }));

    instance = new ExchangeBootstrap(fleetCrewExchange);

    return instance;
  }

  private ExchangeBootstrap(final Exchange ...exchanges) {
    this.dispatcher = new ExchangeDispatcher(exchanges);
  }

  public Dispatcher dispatcher() {
    return dispatcher;
  }

}