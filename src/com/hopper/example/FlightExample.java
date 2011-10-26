package com.hopper.example;
	 	
import java.io.IOException;
import com.hopper.models.flights.FlightProtocols.Trip;
import com.hopper.models.flights.FlightProtocols.Trip.FlightFare;
import com.hopper.models.flights.FlightProtocols.Trip.FlightFare.FlightSegment;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
	 	
public class FlightExample {
    static final String EXCHANGE_NAME = "hopper.topic";
    static final String ROUTING_KEY = "partner.flights";
    private Channel _queueChannel;

    /**
     * Initialization of the connection to the MQ channel.
     * Should not be done for every message
     * @throws IOException
     */
    private void initChannel() throws IOException {
    ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        _queueChannel = connection.createChannel();
        // Ensure the Exchange Exists, create it if it doesn't
        _queueChannel.exchangeDeclare(EXCHANGE_NAME, "topic", true); 
    }

    public Channel getChannel() {
      return _queueChannel;
    }

   @Override
   protected void finalize() throws Throwable {
      _queueChannel.close();
      _queueChannel.getConnection().close();
   }

  /**
   *  This is the code that gets called every time you want to publish a message to the queue
   *  @throws IOException
   */
  public synchronized void testPublish(Trip trip) throws IOException {
    // You will want to implement some kind of retry mechanism, which will also
    // re-initialize the channel in the case of timeout or disconnect.
    _queueChannel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, trip.toByteArray());
  }

  /**
   * Example of how to build a FlightFare message object.
   * This example uses hard-coded dummy data.
   * @return
   */
  private static Trip buildExampleTrip() {
    // Create one segment for a direct flight
    // (i.e. create two segments if there is one stop)
    FlightSegment outboundSegment = FlightSegment.newBuilder()
      .setDepartureDate(1302797128156L)
      .setArrivalDate(1302745334219L)
      .setOrigin("ORD")
      .setDestination("YUL")
      .setCarrierCode("AC")
      .setFlightNumber("545")
      .build();

    FlightSegment inboundSegment = FlightSegment.newBuilder()
      .setDepartureDate(1302745388219L)
      .setArrivalDate(1302745978219L)
      .setOrigin("YUL")
      .setDestination("ORD")
      .setCarrierCode("AC")
      .setFlightNumber("546")
      .build();

    // Create a fare for the one-way segment(s)
    FlightFare flightFare = 
      FlightFare.newBuilder()
        .setOrigin("ORD")
        .setDestination("YUL")
        .addSegment(outboundSegment)
        .addSegment(inboundSegment)
        .setSourceId("1P") // Worldspan
        .setFareCode("VYXAP2")
        .setCurrencyCode("USD")
        .setBaseAmount(1055.22D)
        .setTaxAmount(312.20D)
        .build();

    // Create a trip for this fare (one way, one segment).
    Trip trip = 
      Trip.newBuilder()
      .addFare(flightFare)
      .setMerchantId("ZZZZ") // This trip is sold by the merchant identified by code ZZZZ
      .setTimestamp(System.currentTimeMillis()) // Offered Now
 .setBookingPath("https://www.zzzz.com/viewFlights?retrieveParams=true&z=9dd8&r=d2&z=9dd9&r=d3")
      .build();
    return trip;
  }

  public static final void main(String[] args) throws Exception {
    FlightExample test = new FlightExample();
    test.initChannel();
    // Create a queue and binding that we will use for this test
    // Normally this is done manually by administrators in the RabbitMQ web admin
    final String TEST_QUEUE = "test_queue";
    test.getChannel().queueDeclare(TEST_QUEUE, true, false, false, null);
    test.getChannel().queueBind(TEST_QUEUE, EXCHANGE_NAME, ROUTING_KEY); 
    // Test the publishing of a trip message.  It gets routed to the queue according to the routing key.
    test.testPublish(buildExampleTrip());
    // This is the code that would normally only get called by the Process reading from the queue.
    QueueingConsumer consumer = new QueueingConsumer(test.getChannel());
    test.getChannel().basicConsume(TEST_QUEUE, true, consumer);
    Delivery delivery = consumer.nextDelivery();
    Trip trip = Trip.parseFrom(delivery.getBody());
    System.out.println(trip);
    System.exit(0);
  }
}