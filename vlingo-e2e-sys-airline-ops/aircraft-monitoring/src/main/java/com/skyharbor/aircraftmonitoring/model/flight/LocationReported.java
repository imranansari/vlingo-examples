package com.skyharbor.aircraftmonitoring.model.flight;

import java.util.UUID;
import io.vlingo.common.version.SemanticVersion;
import io.vlingo.lattice.model.IdentifiedDomainEvent;

public final class LocationReported extends IdentifiedDomainEvent {

  private final UUID eventId;
  public final String id;
  public final Aircraft aircraft;
  public final Location location;
  public final ActualDeparture actualDeparture;
  public final EstimatedArrival estimatedArrival;
  public final ActualArrival actualArrival;

  public LocationReported(final FlightState state) {
    super(SemanticVersion.from("0.0.1").toValue());
    this.id = state.id;
    this.aircraft = state.aircraft;
    this.location = state.location;
    this.actualDeparture = state.actualDeparture;
    this.estimatedArrival = state.estimatedArrival;
    this.actualArrival = state.actualArrival;
    this.eventId = UUID.randomUUID(); //TODO: Define the event id
  }

  @Override
  public String identity() {
    return eventId.toString();
  }
}
