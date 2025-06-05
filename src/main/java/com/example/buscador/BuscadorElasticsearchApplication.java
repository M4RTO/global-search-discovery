package com.example.buscador;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

public class BuscadorElasticsearchApplication {

	public record ItemMCC(int idArticle, int colorId, int model, int quality, float price, String description) {}


	public static void main(String[] args) throws IOException {
		// ⚙️ 1. Conexión con Elasticsearch
		RestClient restClient = RestClient.builder(
				new HttpHost("localhost", 9200)
		).build();

		ElasticsearchClient esClient = new ElasticsearchClient(
				new RestClientTransport(restClient, new JacksonJsonpMapper())
		);

		String indexName = "items_mcc";

		// ⚠️ 2. Borrar el índice si existe
		boolean exists = esClient.indices().exists(ExistsRequest.of(e -> e.index(indexName))).value();
		if (exists) {
			esClient.indices().delete(DeleteIndexRequest.of(d -> d.index(indexName)));
			System.out.println("Índice borrado.");
		}

		//  3. Crear el índice y mapear campos como texto
		esClient.indices().create(CreateIndexRequest.of(c -> c
				.index(indexName)
				.mappings(m -> m
						.properties("idArticle", p -> p.text(t -> t))
						.properties("colorId", p -> p.text(t -> t))
						.properties("model", p -> p.text(t -> t))
						.properties("quality", p -> p.text(t -> t))
						.properties("price", p -> p.float_(f -> f))  // lo dejamos como número
						.properties("description", p -> p.text(t -> t))
				)
		));

		//  4. Insertar datos de prueba
		List<ItemMCC> items = List.of(
				new ItemMCC(1, 1, 1, 1, 10.0f, "Camiseta roja"),
				new ItemMCC(2, 2, 2, 2, 20.0f, "Camiseta azul"),
				new ItemMCC(3, 3, 3, 3, 30.0f, "Camiseta verde"),
				new ItemMCC(4, 4, 4, 4, 40.0f, "Camiseta negra"),
				new ItemMCC(5, 5, 5, 5, 50.0f, "Camiseta amarilla"),
				new ItemMCC(6, 6, 6, 6, 60.0f, "Camiseta marron"),
				new ItemMCC(7, 7, 7, 7, 70.0f, "Camiseta blanca"),
				new ItemMCC(8, 8, 8, 8, 80.0f, "Camiseta gris"),
				new ItemMCC(9, 9, 9, 9, 90.0f, "Camiseta rosa"),
				new ItemMCC(10, 10, 10, 10, 100.0f, "Camiseta celeste"),
				new ItemMCC(9090, 3429, 123590, 90238, 200.0f, "Camiseta tyron")
		);

		for (ItemMCC item : items) {
			esClient.index(IndexRequest.of(i -> i
					.index(indexName)
					.id(item.idArticle() + "-" + item.colorId())
					.document(item)
			));
		}

		System.out.println("Datos indexados correctamente.\n");

		//  5. Consulta interactiva en consola
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("🔍 Ingrese texto a buscar (ENTER para salir): ");
			String valor = scanner.nextLine();
			if (valor.isBlank()) break;

			//  Consulta de tipo "match" para buscar en todos los campos
			Query queryLike = Query.of(q -> q
					.queryString(qs -> qs
							.fields("idArticle", "colorId", "model", "quality", "description")
							.query("*" + valor + "*")
					)
			);


			//  Combinar ambas consultas con un OR (bool.should)
			Query finalQuery = Query.of(q -> q
					.bool(b -> b
							.should(queryLike)
							.should(Query.of(f -> f.fuzzy(fuzzy -> fuzzy
									.field("description")
									.value(valor)
									.fuzziness("AUTO")
							)))
					)
			);

			SearchResponse<ItemMCC> response = esClient.search(s -> s
							.index(indexName)
							.query(finalQuery)
							.size(20)  // limite de resultados
					, ItemMCC.class);

			List<Hit<ItemMCC>> hits = response.hits().hits();
			if (hits.isEmpty()) {
				System.out.println("❌ No se encontraron resultados.");
			} else {
				System.out.println("✅ Resultados encontrados:");
				for (Hit<ItemMCC> hit : hits) {
					System.out.println(hit.source());
				}
			}
			System.out.println();
		}

		// 🔚 Cerrar cliente
		restClient.close();
	}
}
