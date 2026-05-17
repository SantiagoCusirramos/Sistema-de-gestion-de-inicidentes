package View;

import Controller.IncidenteController;
import Model.Incidente;
import Model.Usuario;
import Service.ReporteService;
import enums.EstadoIncidente;
import enums.Prioridad;
import enums.RolUsuario;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Correcciones aplicadas:
 *  [BUG-012] crearGraficos() ahora llama a los métodos de ReporteService que
 *             aceptan usuarioId + rol, de modo que un TECNICO solo ve las
 *             estadísticas de sus propios incidentes asignados, no las globales.
 *  [BUG-012] crearTablaRecientes() usa getIncidentesRecientes(limite, id, rol)
 *             en lugar del método legacy que devuelve todos los incidentes.
 */
public class ReporteView {

    private ReporteService reporteService;
    private IncidenteController incidenteController;
    private Usuario usuarioActual;

    public ReporteView(Usuario usuarioActual) {
        this.reporteService      = new ReporteService();
        this.incidenteController = new IncidenteController(usuarioActual);
        this.usuarioActual       = usuarioActual;
    }

    public Node getContenido() {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));

        Label lblTitulo = new Label("Reportes y Estadisticas");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 18));

        // Subtítulo que indica el alcance de los datos según el rol
        Label lblAlcance = new Label(obtenerTextoAlcance());
        lblAlcance.setStyle("-fx-text-fill: #555555; -fx-font-style: italic;");

        root.getChildren().addAll(
                lblTitulo,
                lblAlcance,
                crearResumenGeneral(),
                crearGraficos(),
                crearTablaRecientes()
        );
        return root;
    }

    /** Texto descriptivo que indica si los datos son globales o filtrados. */
    private String obtenerTextoAlcance() {
        if (usuarioActual.getRol() == RolUsuario.ADMIN) {
            return "Mostrando estadísticas globales del sistema (todos los incidentes)";
        } else {
            return "Mostrando estadísticas de tus incidentes asignados";
        }
    }

    private HBox crearResumenGeneral() {
        // incidenteController ya filtra por rol internamente
        int total = incidenteController.getTotalIncidentes();

        long pendientes = incidenteController.getIncidentesPorEstado(EstadoIncidente.PENDIENTE);
        long enProceso  = incidenteController.getIncidentesPorEstado(EstadoIncidente.EN_PROCESO);
        long resueltos  = incidenteController.getIncidentesPorEstado(EstadoIncidente.RESUELTO);
        long cerrados   = incidenteController.getIncidentesPorEstado(EstadoIncidente.CERRADO);

        VBox cardTotal      = crearCard("Total",      String.valueOf(total),      "#1a237e");
        VBox cardPendientes = crearCard("Pendientes", String.valueOf(pendientes), "#e65100");
        VBox cardProceso    = crearCard("En Proceso", String.valueOf(enProceso),  "#1565c0");
        VBox cardResueltos  = crearCard("Resueltos",  String.valueOf(resueltos),  "#2e7d32");
        VBox cardCerrados   = crearCard("Cerrados",   String.valueOf(cerrados),   "#6a1b9a");

        HBox cards = new HBox(12,
            cardTotal, cardPendientes, cardProceso, cardResueltos, cardCerrados);
        cards.setPadding(new Insets(10, 0, 10, 0));
        return cards;
    }

    private VBox crearCard(String titulo, String valor, String color) {
        Label lblTitulo = new Label(titulo);
        lblTitulo.setFont(Font.font("Arial", FontWeight.NORMAL, 12));
        lblTitulo.setStyle("-fx-text-fill: white;");

        Label lblValor = new Label(valor);
        lblValor.setFont(Font.font("Arial", FontWeight.BOLD, 28));
        lblValor.setStyle("-fx-text-fill: white;");

        VBox card = new VBox(5, lblTitulo, lblValor);
        card.setPadding(new Insets(15, 20, 15, 20));
        card.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        card.setPrefWidth(160);
        return card;
    }

    private HBox crearGraficos() {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Incidentes por Prioridad");
        pieChart.setLegendSide(Side.BOTTOM);
        pieChart.setPrefWidth(350);
        pieChart.setPrefHeight(300);

        // [BUG-012] Usar método con rol para que TECNICO no vea datos globales
        Map<Prioridad, Integer> stats = reporteService.getIncidentesPorPrioridad(
            usuarioActual.getId(), usuarioActual.getRol());

        for (Map.Entry<Prioridad, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                pieChart.getData().add(new PieChart.Data(
                    entry.getKey().name() + " (" + entry.getValue() + ")",
                    entry.getValue()));
            }
        }

        if (pieChart.getData().isEmpty()) {
            pieChart.getData().add(new PieChart.Data("Sin datos", 1));
        }

        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Estado");

        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Cantidad");

        BarChart<String, Number> barChart = new BarChart<>(ejeX, ejeY);
        barChart.setTitle("Incidentes por Estado");
        barChart.setPrefWidth(400);
        barChart.setPrefHeight(300);
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> serie = new XYChart.Series<>();

        // incidenteController.listarIncidentes() ya filtra por rol
        List<Incidente> todos = incidenteController.listarIncidentes();
        for (EstadoIncidente estado : EstadoIncidente.values()) {
            long count = todos.stream().filter(i -> i.getEstado() == estado).count();
            serie.getData().add(new XYChart.Data<>(estado.name(), count));
        }
        barChart.getData().add(serie);

        HBox graficos = new HBox(20, pieChart, barChart);
        graficos.setPadding(new Insets(10, 0, 10, 0));
        return graficos;
    }

    @SuppressWarnings("unchecked")
    private VBox crearTablaRecientes() {
        Label lblTitulo = new Label("Incidentes Recientes");
        lblTitulo.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        TableView<Incidente> tabla = new TableView<>();
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tabla.setPrefHeight(200);
        tabla.setPlaceholder(new Label("No hay incidentes recientes"));

        TableColumn<Incidente, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(50);

        TableColumn<Incidente, String> colTituloCol = new TableColumn<>("Titulo");
        colTituloCol.setCellValueFactory(new PropertyValueFactory<>("titulo"));

        TableColumn<Incidente, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEstado().name()));

        TableColumn<Incidente, String> colPrioridad = new TableColumn<>("Prioridad");
        colPrioridad.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getPrioridad().name()));

        TableColumn<Incidente, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(cd ->
            new SimpleObjectProperty<>(cd.getValue().getFechaCreacion().toLocalDate()));

        tabla.getColumns().addAll(colId, colTituloCol, colEstado, colPrioridad, colFecha);

        // [BUG-012] Usar método con rol para filtrar correctamente
        List<Incidente> recientes = reporteService.getIncidentesRecientes(
            15, usuarioActual.getId(), usuarioActual.getRol());
        tabla.setItems(FXCollections.observableArrayList(recientes));

        VBox panel = new VBox(8, lblTitulo, tabla);
        panel.setPadding(new Insets(10, 0, 0, 0));
        return panel;
    }
}
