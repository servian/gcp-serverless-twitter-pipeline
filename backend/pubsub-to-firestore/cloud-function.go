package p

import (
	"context"
	"encoding/json"
	"log"

	"cloud.google.com/go/firestore"
	"cloud.google.com/go/pubsub"
)

func SaveToFirestore(ctx context.Context, data map[string]interface{}) error {
	// Get a Firestore client.
	client, err := firestore.NewClient(ctx, "chris-tippett-app-gartner-2019")
	if err != nil {
		log.Fatalf("Failed to create client: %v", err)
	}
	// Close client when done.
	defer client.Close()

	// Add data
	_, _, err = client.Collection("tweets").Add(ctx, data)
	if err != nil {
		jsonData, _ := json.Marshal(data)
		log.Printf(string(jsonData))
		log.Fatalf("Failed adding tweet: %v", err)
	}
	return nil
}

func PubSubConsumer(ctx context.Context, m *pubsub.Message) error {
	var data map[string]interface{}
	if err := json.Unmarshal(m.Data, &data); err != nil {
		log.Fatalf("Failed to decode Pub/Sub payload: %v", err)
	}
	delete(data, "place") // avoid issue with Firestore nested data type
	SaveToFirestore(ctx, data)
	return nil
}
