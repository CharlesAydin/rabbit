# Rabbit

![Hopper logo](http://www.hopper.com/cms-assets/files/icons/logotype.png)

Hopper Rabbit is a push messaging API to allow airfare distributors and merchants to stream priced itineraries to the Hopper travel analytics engine. This engine powers recommendations and predictions for Hopper's [Android](https://play.google.com/store/apps/details?id=com.hopper.mountainview.play&hl=en) and [Apple iOS](https://itunes.apple.com/us/app/hopper-airfare-predictions/id904052407?mt=8) apps.

For details about connecting to the live Hopper Rabbit listener service to include your company's data in Hopper's apps, please contact Hopper by e-mail at info@hopper.com.

##Transport Protocol
Qualified partners are provided a single HTTPS listener URL. Messages are batched and sent via a POST transaction to the Hopper listener URL. The listener service supports HTTP/1.1 pipelining.

Messages are serialized using [ProtocolBuffers](https://developers.google.com/protocol-buffers/?hl=en). ProtocolBuffers is simple, programming language-agnostic and much more IO-efficient than XML, which is important considering the message volume that Hopper consumes (billions of messages / day).

##Triggers
The Hopper Rabbit API is a Push API, which implies that the data provider must initiate messages according to its own triggering logic.

The recommended strategy is to trigger a Trip message for each of the best available fares returned to a customer query (against the data provider's system) for flights between an Origin and Destination for specific departure and return dates. This strategy ensures that the coverage and freshness of the push stream is consistent with real customer demand.

The following recommendations are designed to limit message traffic to the minimum required by the Hopper analytics engine:
 
  1. It is recommended that data providers trigger no more than **50 messages** per client request. In case of truncation, messages should be ordered according to price.
  2. It is recommended that identical client requests received within **10 minutes** do not trigger messages. That is to say, for a given Origin-Destination-DepartureDate-ReturnDate request, messages are not triggered if a previous identical rquest triggered messages less than 10 minutes ago, even if the results are different.

Dropped messages are considered acceptable for short periods of time (minutes). In the case of excessive message backlog due to communication failure, it is recommended that messages be dropped. It is recommended that any message buffer have a maximum length of no more than 1 million messages before truncation.

##Airfare Messages
For flight data, Hopper currently supports one type of root message: **TripBatch**, defined in the [flights.proto](https://github.com/hopper/rabbit/blob/master/protocols/flights.proto) file:

###TripBatch
The **TripBatch** message represents a batch of **Trip** messages that are bundled together for more efficient network processing. Trip message in a TripBatch will be processed independently from one another by Hopper. That is to say, they may include trips for one or more customer queries.

Fieldname | Type | Required | Collection | Description
:---------|:----:|:--------:|:----------:|:---------- 
trips     | Trip | **yes**  | **yes**    | An unordered list of trips

###Trip
The **Trip** message represents a single bookable itinerary shopped by a customer on a data provider's system. It can represent a one-way itinerary, a return itinerary or a multi-stop itinerary.

Fieldname    | Type     | Required | Collection | Description
:------------|:--------:|:--------:|:----------:|:---------- 
fare         |FlightFare| **yes**  | no         | A priced unit of travel consisting of one or many segments
merchant_id  |string    | **yes**  | no         | The unique ID of the data provider
timestamp    |long      | **yes**  | no         | The date/time that this trip was shopped. A 64-bit long representing milliseconds since midnight, Jan 1, 1970 UTC
booking_path |string    | no       | no         | An optional deep-link URL where this trip can be booked on the merchant site.

##FlightFare
The **FlightFare** class represents an ordered list of flight segments that are priced together as a bookable itinerary.

Fieldname    | Type     | Required | Collection | Description
:------------|:--------:|:--------:|:----------:|:---------- 
segment      |FlightSegment|**yes**| **yes**    | An ordered list of flight segments that make up this itinerary
currency_code|string    | **yes**  | no         | The [ISO 4217](https://en.wikipedia.org/wiki/ISO_4217) currency code for all price amounts in this fare
base_amount  |double    | **yes**  | no         | The total base fare amount for this itinerary
tax_amount   |double    | **yes**  | no         | The total taxes and fees amount for this itinerary (includes surcharge_amount below)
surcharge_amount|double | no       | no         | The total YQ and YR fees for this itinerary
pax_type     |string    | **yes**  | no         | The passenger type code for which this fare applies (e.g. **ADT**, **CHD**, **INF**)
refundable   |boolean   | no       | no         | An optional flag indicating that this fare is fully refundable. Defaults to false.
source_id    |string    | no       | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the validating carrier
origin       |string    | no       | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the departure airport of the first outbound segment of the itinerary.
destination  |string    | no       | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the arrival airport of the last outbound segment of a one-way or return itinerary. Leave blank in the case of multi-destination fares.

##FlightSegment
The **FlightSegment** class represents a single flight leg on a single aircraft between two airports.

Fieldname    | Type     | Required | Collection | Description
:------------|:--------:|:--------:|:----------:|:---------- 
carrier_code | string   | **yes**  | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the marketing carrier
flight_number| string   | **yes**  | no         | [Flight number](https://en.wikipedia.org/wiki/Flight_number). Usually between 1 and 4 digits.
origin       | string   | **yes**  | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the departure airport
destination  | string   | **yes**  | no         | [IATA Code](http://www.iata.org/publications/Pages/code-search.aspx) of the arrival airport
departure_date| long    | **yes**  | no         | The departure date/time of this flight. A 64-bit long representing milliseconds since midnight, Jan 1, 1970 UTC
arrival_date | long     | **yes**  | no         | The arrival date/time of this flight. A 64-bit long representing milliseconds since midnight, Jan 1, 1970 UTC
stops        | integer  | no       | no         | The number of stops on this flight segment. Usuall 0 unless the aircraft makes a stop, e.g. to refuel.
outgoing     | boolean  | no       | no         | Boolean flag indicating that this segment belongs to the outbound portion of a roundtrip fare.
fare_code    | string   | no       | no         | The fare basis code applied to price this itinerary, e.g. **KL14LNR**
cabin_class  | string   | no       | no         | The cabin class code for which this fare applies, e.g. **Y**
available_seats| integer| no       | no         | The number of remaining seats for this fare at the time it was shopped. 
