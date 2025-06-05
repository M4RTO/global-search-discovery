package com.example.buscador;

import org.apache.lucene.analysis.es.SpanishAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class BuscadorLuceneApplication {


	//  Clase de datos a indexar
	public record ItemMCC(int idArticle, int colorId, int model, int quality, float price, String description) {}

	public static void main(String[] args) throws Exception {
		//  1. Crear analizador y directorio en memoria
		//Analyzer analyzer = new StandardAnalyzer();
		// analizador en español
		Analyzer analyzer = new SpanishAnalyzer();

		//Directory index = FSDirectory.open(Paths.get("indice-lucene"));
		Directory index = new ByteBuffersDirectory();

		//  2. Configurar IndexWriter
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		IndexWriter writer = new IndexWriter(index, config);

		// podam factory- lib para generar datos aleatorios


		//  3. Lista de elementos a indexar
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

		//  4. Indexar los documentos
		for (ItemMCC item : items) {
			agregarDocumento(writer, item);
		}

		writer.close();

		//  5. Preparar buscador
		DirectoryReader reader = DirectoryReader.open(index);
		IndexSearcher searcher = new IndexSearcher(reader);

		BufferedReader consola = new BufferedReader(new InputStreamReader(System.in));
		System.out.println("🔍 Buscador de ItemMCC (escribe 'salir' para terminar)");

		while (true) {
			System.out.print("\nConsulta: ");
			String linea = consola.readLine();

			if (linea == null || linea.trim().equalsIgnoreCase("salir")) {
				break;
			}

			String valor = linea.trim().toLowerCase();

			//  Campos a buscar
			String[] campos = {"idArticle", "colorId", "model", "quality", "price", "description"};

			//  Construir una consulta que busque en todos los campos
			BooleanQuery.Builder builder = new BooleanQuery.Builder();


			for (String campo : campos) {
				if ("description".equals(campo)) {
					try {
						// Usar QueryParser para descripción para permitir búsquedas complejas
						// QueryParser parser = new QueryParser("description", analyzer);
						// parser.setDefaultOperator(QueryParser.Operator.AND);
						// Query parsedQuery = parser.parse(valor);
						// builder.add(parsedQuery, BooleanClause.Occur.SHOULD);

						// Agregar FuzzyQuery para búsqueda aproximada en descripción
						builder.add(new FuzzyQuery(new Term("description", valor), 2), BooleanClause.Occur.SHOULD);
					} catch (Exception e) {
						System.err.println("Error parsing description query: " + e.getMessage());
					}
				} else {
					// Para campos numéricos guardados como string: usar TermQuery
					builder.add(new PrefixQuery(new Term(campo, valor)), BooleanClause.Occur.SHOULD);
				}
			}

			Query query = builder.build();
			//"Ejecuta la búsqueda y devuélveme como máximo 10 documentos que coincidan con la consulta."
			TopDocs resultados = searcher.search(query, 10);

			// ✅ Mostrar resultados
			if (resultados.totalHits.value > 0) {
				System.out.println("Resultados encontrados: " + resultados.totalHits.value);
				for (ScoreDoc hit : resultados.scoreDocs) {
					Document doc = searcher.doc(hit.doc);
					System.out.printf(" -  idArticle: %s, colorId: %s, model: %s, quality: %s, price: %s, description: %s%n",
							doc.get("idArticle"),
							doc.get("colorId"),
							doc.get("model"),
							doc.get("quality"),
							doc.get("price"),
							doc.get("description"));
				}
			} else {
				System.out.println("❌ No se encontraron resultados.");
			}
		}

		reader.close();
		index.close();
		System.out.println("👋 Buscador finalizado.");
	}

	// Método que agrega un documento a índice
	private static void agregarDocumento(IndexWriter writer, ItemMCC item) throws Exception {
		Document doc = new Document();

		// Guardar campos numéricos como StringField (exact match)
		doc.add(new StringField("idArticle", String.valueOf(item.idArticle()), Field.Store.YES));
		doc.add(new StringField("colorId", String.valueOf(item.colorId()), Field.Store.YES));
		doc.add(new StringField("model", String.valueOf(item.model()), Field.Store.YES));
		doc.add(new StringField("quality", String.valueOf(item.quality()), Field.Store.YES));
		doc.add(new StringField("price", String.valueOf(item.price()), Field.Store.YES));

		// Campo descripción como TextField para análisis y búsqueda textual
		doc.add(new TextField("description", item.description(), Field.Store.YES));

		writer.addDocument(doc);
	}
}
