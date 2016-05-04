/*
 *                        e  ohe
 *                       ohh  hhp
 *                       ohhh hhh
 *      ,phh.             ohhoehh,
 *     :hhhhhhhhhhhhhhh~   hhh hh,
 *     :hhhhhhhhhhhhhhhhhe  hhoeh,
 *        hhhhhhhhhhhhhhhhhh ehhhe,
 *      ehhhhhhhhhhhhhhhhhhhhhhhhhhp
 *    rhhhhhhhhhhhhhhhhhhhhhhhhhhhhhh
 *   phhhhhhe:,    ,,ehhhhhhhhhhhhhhhp
 *  hhhh~                :hhhhhhhhhhe
 *  hh~                    ,hhhhh,,
 *                            hhhhe
 *                             hhhho
 *                              ,hhhh
 *                               ~hhh
 */
package com.hopper.rabbit.example;

import java.io.IOException;
import com.hopper.models.flights.FlightProtocols.TripBatch;
import com.hopper.models.flights.FlightProtocols.Trip;
import com.hopper.models.flights.FlightProtocols.Trip.FlightFare;
import com.hopper.models.flights.FlightProtocols.Trip.FlightFare.FlightSegment;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author joost
 */
public class HopperFeedClient {

  /**
   * Example of how to build a FlightFare message object.
   * This example uses hard-coded dummy data.
   * @return
   */
  private static TripBatch buildExampleTripBatch() {
    // First build all the segments for this trip
    FlightSegment outboundSegment =
      FlightSegment.newBuilder()
        .setCarrierCode("AC")
        .setFlightNumber("545")
        .setOrigin("ORD")
        .setDestination("YUL")
        .setDepartureDate(1302797128156L)
        .setArrivalDate(1302745334219L)
        .setOutgoing(true)
        .setFareCode("VYXAP2")
        .setCabinClass("V")
        .setAvailableSeats(7)
        .build();

    FlightSegment inboundSegment =
      FlightSegment.newBuilder()
        .setCarrierCode("AC")
        .setFlightNumber("547")
        .setOrigin("YUL")
        .setDestination("ORD")
        .setDepartureDate(1302797146231L)
        .setArrivalDate(1302797154231L)
        .setOutgoing(false)
        .setFareCode("VYXAP2")
        .setCabinClass("V")
        .setAvailableSeats(3)
        .build();

    // Create a fare for this roundtrip itinerary
    FlightFare flightFare =
      FlightFare.newBuilder()
        .addSegment(outboundSegment)
        .addSegment(inboundSegment)
        .setCurrencyCode("USD")
        .setBaseAmount(1055.22D)
        .setTaxAmount(312.20D)
        .setSurchargeAmount(132.10D)
        .setPaxType("ADT")
        .setOrigin("ORD")
        .setDestination("YUL")
        .setPosCountryCode("US")
        .build();

    // Create a trip for this fare
    Trip trip =
      Trip.newBuilder()
        .setFare(flightFare)
        .setMerchantId("MMYT") // This trip is sold by the merchant identified by code ZZZZ
        .setTimestamp(System.currentTimeMillis()) // Offered Now
        .build();

    TripBatch tripBatch =
      TripBatch.newBuilder()
        .addTrips(trip) // Normally you would add more than one trip to a batch
        .build();

    return tripBatch;
  }

  /**
   *  This is the code that gets called every time you want to publish a message to the queue
   *  @throws IOException
   */
  public synchronized void send(TripBatch tripBatch) throws IOException {
    URL url = new URL("https://api.hopper.com/xxx");
    HttpURLConnection urlc = (HttpURLConnection) url.openConnection();
    urlc.setDoOutput(true);
    urlc.setRequestMethod("POST");
    urlc.setRequestProperty("X-Point-Of-Sale", "US");
    urlc.setRequestProperty("Content-Type", "application/x-protobuf");
    urlc.setFixedLengthStreamingMode(tripBatch.getSerializedSize());
    tripBatch.writeTo(urlc.getOutputStream());
  }

  public static final void main(String[] args) throws Exception {
    HopperFeedClient testClient = new HopperFeedClient();
    TripBatch exampleTripBatch = buildExampleTripBatch();
    testClient.send(exampleTripBatch);
    System.exit(0);
  }
}
