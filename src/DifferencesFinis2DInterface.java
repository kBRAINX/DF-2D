import javax.swing.*;
import java.awt.*;

/**
 * Interface utilisateur principale pour le solveur de différences finies 2D
 *
 * Cette interface permet de :
 * - Configurer les paramètres du problème (cas de test, taille de maillage)
 * - Sélectionner la méthode de résolution (Gauss-Seidel classique, avec relaxation, parallélisé)
 * - Lancer les calculs et suivre leur progression
 * - Visualiser les résultats avec différents types d'affichage
 * - Analyser les erreurs et la convergence
 * - Comparer les performances des méthodes
 */
public class DifferencesFinis2DInterface extends JFrame {

    // Constantes d'interface
    private static final int LARGEUR_FENETRE = 1400;
    private static final int HAUTEUR_FENETRE = 900;

    // Composants GUI principaux
    private JPanel panelControles;
    private JPanel panelVisualisations;
    private JTextArea zoneResultats;
    private JProgressBar barreProgres;
    private JLabel labelStatut;

    // Contrôles de configuration
    private JComboBox<Maillage.CasTest> comboCasTest;
    private JSpinner spinnerTailleMaillage;
    private JComboBox<SolveurGaussSeidel.MethodeResolution> comboMethode;
    private JSpinner spinnerTolerance;
    private JSpinner spinnerMaxIterations;
    private JSpinner spinnerOmega;
    private JLabel labelOmega;

    // Boutons d'action
    private JButton boutonResoudre;
    private JButton boutonAnalyserConvergence;
    private JButton boutonComparerMethodes;
    private JButton boutonReset;
    private JButton boutonExporter;

    // Panels de visualisation
    private VisualiseurGraphique.PanelVisualisation panelColoration;
    private VisualiseurGraphique.PanelVisualisation panelContours;

    // Données du problème
    private Maillage maillageActuel;
    private SolveurGaussSeidel solveur;
    private AnalyseurErreurs.ResultatAnalyse derniereAnalyse;

    /**
     * Constructeur de l'interface principale
     */
    public DifferencesFinis2DInterface() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        resetToDefault();

        System.out.println("Interface utilisateur initialisée");
    }

    /**
     * Initialise tous les composants GUI
     */
    private void initializeComponents() {
        setTitle("Solveur Différences Finies 2D - Équation -ΔU = f");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(LARGEUR_FENETRE, HAUTEUR_FENETRE);
        setLocationRelativeTo(null);

        // Initialisation du solveur
        solveur = new SolveurGaussSeidel();

        // === Panel de contrôles ===
        panelControles = new JPanel();
        panelControles.setBorder(BorderFactory.createTitledBorder("Configuration"));
        panelControles.setPreferredSize(new Dimension(350, 0));

        // Sélection du cas de test
        comboCasTest = new JComboBox<>(Maillage.CasTest.values());
        comboCasTest.setToolTipText("Sélectionnez le cas de test à résoudre");

        // Taille du maillage
        spinnerTailleMaillage = new JSpinner(new SpinnerNumberModel(20, 5, 200, 5));
        spinnerTailleMaillage.setToolTipText("Nombre de points intérieurs par direction (N)");

        // Méthode de résolution
        comboMethode = new JComboBox<>(SolveurGaussSeidel.MethodeResolution.values());
        comboMethode.setToolTipText("Méthode de résolution du système linéaire");

        // Paramètres de convergence
        spinnerTolerance = new JSpinner(new SpinnerNumberModel(1e-6, 1e-12, 1e-3, 1e-6));
        spinnerTolerance.setEditor(new JSpinner.NumberEditor(spinnerTolerance, "0.######E0"));

        spinnerMaxIterations = new JSpinner(new SpinnerNumberModel(5000, 100, 50000, 500));

        // Paramètre de relaxation (pour SOR)
        spinnerOmega = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.9, 0.1));
        spinnerOmega.setEditor(new JSpinner.NumberEditor(spinnerOmega, "0.##"));
        labelOmega = new JLabel("Paramètre ω (relaxation):");

        // Boutons d'action
        boutonResoudre = new JButton("Résoudre");
        boutonResoudre.setFont(new Font("Arial", Font.BOLD, 12));
        boutonResoudre.setBackground(new Color(100, 150, 100));
        boutonResoudre.setForeground(Color.WHITE);

        boutonAnalyserConvergence = new JButton("Analyser Convergence");
        boutonComparerMethodes = new JButton("Comparer Méthodes");
        boutonReset = new JButton("Reset");
        boutonExporter = new JButton("Exporter Résultats");

        // Barre de progression et statut
        barreProgres = new JProgressBar();
        barreProgres.setStringPainted(true);
        barreProgres.setVisible(false);

        labelStatut = new JLabel("Prêt");
        labelStatut.setBorder(BorderFactory.createEtchedBorder());

        // === Zone de résultats ===
        zoneResultats = new JTextArea();
        zoneResultats.setFont(new Font("Courier New", Font.PLAIN, 11));
        zoneResultats.setEditable(false);
        zoneResultats.setBackground(Color.BLACK);

        // === Panels de visualisation ===
        panelColoration = new VisualiseurGraphique.PanelVisualisation(400, 400);
        panelContours = new VisualiseurGraphique.PanelVisualisation(400, 400);

        panelVisualisations = new JPanel(new GridLayout(1, 2, 10, 10));
        panelVisualisations.setBorder(BorderFactory.createTitledBorder("Visualisations"));

        // Configuration initiale des visualisations
        panelColoration.setTypeVisualisation(VisualiseurGraphique.TypeVisualisation.COLORATION);
        panelContours.setTypeVisualisation(VisualiseurGraphique.TypeVisualisation.COURBES_NIVEAU);
    }

    /**
     * Configure la disposition des composants
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // === Layout du panel de contrôles ===
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));

        // Section: Problème
        JPanel sectionProbleme = new JPanel(new GridBagLayout());
        sectionProbleme.setBorder(BorderFactory.createTitledBorder("Problème"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        sectionProbleme.add(new JLabel("Cas de test:"), gbc);
        gbc.gridx = 1;
        sectionProbleme.add(comboCasTest, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        sectionProbleme.add(new JLabel("Taille maillage (N):"), gbc);
        gbc.gridx = 1;
        sectionProbleme.add(spinnerTailleMaillage, gbc);

        // Section: Méthode
        JPanel sectionMethode = new JPanel(new GridBagLayout());
        sectionMethode.setBorder(BorderFactory.createTitledBorder("Méthode de Résolution"));

        gbc.gridx = 0; gbc.gridy = 0;
        sectionMethode.add(new JLabel("Méthode:"), gbc);
        gbc.gridx = 1;
        sectionMethode.add(comboMethode, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        sectionMethode.add(new JLabel("Tolérance:"), gbc);
        gbc.gridx = 1;
        sectionMethode.add(spinnerTolerance, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        sectionMethode.add(new JLabel("Max itérations:"), gbc);
        gbc.gridx = 1;
        sectionMethode.add(spinnerMaxIterations, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        sectionMethode.add(labelOmega, gbc);
        gbc.gridx = 1;
        sectionMethode.add(spinnerOmega, gbc);

        // Section: Actions
        JPanel sectionActions = new JPanel(new GridLayout(5, 1, 5, 5));
        sectionActions.setBorder(BorderFactory.createTitledBorder("Actions"));
        sectionActions.add(boutonResoudre);
        sectionActions.add(boutonAnalyserConvergence);
        sectionActions.add(boutonComparerMethodes);
        sectionActions.add(boutonReset);
        sectionActions.add(boutonExporter);

        // Section: Progression
        JPanel sectionProgression = new JPanel(new BorderLayout());
        sectionProgression.setBorder(BorderFactory.createTitledBorder("Progression"));
        sectionProgression.add(barreProgres, BorderLayout.CENTER);
        sectionProgression.add(labelStatut, BorderLayout.SOUTH);

        // Assemblage du panel de contrôles
        panelControles.add(sectionProbleme);
        panelControles.add(Box.createVerticalStrut(10));
        panelControles.add(sectionMethode);
        panelControles.add(Box.createVerticalStrut(10));
        panelControles.add(sectionActions);
        panelControles.add(Box.createVerticalStrut(10));
        panelControles.add(sectionProgression);
        panelControles.add(Box.createVerticalGlue());

        // === Layout des visualisations ===
        JPanel panelColAvecTitre = new JPanel(new BorderLayout());
        panelColAvecTitre.add(new JLabel("Coloration (Heatmap)", JLabel.CENTER), BorderLayout.NORTH);
        panelColAvecTitre.add(panelColoration, BorderLayout.CENTER);

        JPanel panelContAvecTitre = new JPanel(new BorderLayout());
        panelContAvecTitre.add(new JLabel("Courbes de Niveau", JLabel.CENTER), BorderLayout.NORTH);
        panelContAvecTitre.add(panelContours, BorderLayout.CENTER);

        panelVisualisations.add(panelColAvecTitre);
        panelVisualisations.add(panelContAvecTitre);

        // === Layout principal ===
        add(panelControles, BorderLayout.WEST);
        add(panelVisualisations, BorderLayout.CENTER);

        // Zone de résultats dans un onglet en bas
        JTabbedPane onglets = new JTabbedPane();
        onglets.addTab("Résultats Détaillés", new JScrollPane(zoneResultats));
        onglets.setPreferredSize(new Dimension(0, 200));

        add(onglets, BorderLayout.SOUTH);
    }

    /**
     * Configure les gestionnaires d'événements
     */
    private void setupEventHandlers() {
        // Changement de méthode -> affichage/masquage du paramètre ω
        comboMethode.addActionListener(e -> {
            boolean isRelaxation = comboMethode.getSelectedItem() ==
                SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION;
            labelOmega.setVisible(isRelaxation);
            spinnerOmega.setVisible(isRelaxation);
            panelControles.revalidate();
        });

        // Changement de cas de test
        comboCasTest.addActionListener(e -> configurerCasTest());

        // Changement de taille de maillage
        spinnerTailleMaillage.addChangeListener(e -> configurerCasTest());

        // Bouton résoudre
        boutonResoudre.addActionListener(e -> resoudreProbleme());

        // Bouton analyser convergence
        boutonAnalyserConvergence.addActionListener(e -> analyserConvergence());

        // Bouton comparer méthodes
        boutonComparerMethodes.addActionListener(e -> comparerMethodes());

        // Bouton reset
        boutonReset.addActionListener(e -> resetToDefault());

        // Bouton exporter
        boutonExporter.addActionListener(e -> exporterResultats());
    }

    /**
     * Configure le cas de test sélectionné
     */
    private void configurerCasTest() {
        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
        int N = (Integer) spinnerTailleMaillage.getValue();

        if (maillageActuel == null || maillageActuel.getN() != N) {
            maillageActuel = new Maillage(N);
        }

        maillageActuel.configurerCasTest(casTest);

        // Mise à jour de l'affichage
        panelColoration.mettreAJour(maillageActuel, null);
        panelContours.mettreAJour(maillageActuel, null);

        ajouterTexteResultats("Configuration: " + casTest.getDescription() +
            " sur maillage " + (N+2) + "×" + (N+2) + "\n");

        // Calcul du ω optimal pour SOR
        if (comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) {
            double omegaOptimal = SolveurGaussSeidel.calculerOmegaOptimal(N);
            spinnerOmega.setValue(omegaOptimal);
        }

        labelStatut.setText("Cas configuré: " + casTest.name());
    }

    /**
     * Lance la résolution du problème dans un thread séparé
     */
    private void resoudreProbleme() {
        if (maillageActuel == null) {
            configurerCasTest();
        }

        // Désactivation des contrôles pendant le calcul
        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Résolution en cours...");

        // Paramètres de résolution
        SolveurGaussSeidel.MethodeResolution methode =
            (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();
        double tolerance = (Double) spinnerTolerance.getValue();
        int maxIter = (Integer) spinnerMaxIterations.getValue();
        double omega = (Double) spinnerOmega.getValue();

        // Worker thread pour éviter le blocage de l'interface
        SwingWorker<SolveurGaussSeidel.ResultatConvergence, String> worker =
            new SwingWorker<SolveurGaussSeidel.ResultatConvergence, String>() {

                @Override
                protected SolveurGaussSeidel.ResultatConvergence doInBackground() throws Exception {
                    publish("Début de la résolution...");

                    // Callback de progression
                    return solveur.resoudre(maillageActuel, methode, tolerance, maxIter, omega,
                        iteration -> {
                            if (iteration % 200 == 0) {
                                publish("Itération " + iteration);
                            }
                        });
                }

                @Override
                protected void process(java.util.List<String> chunks) {
                    for (String message : chunks) {
                        labelStatut.setText(message);
                    }
                }

                @Override
                protected void done() {
                    try {
                        SolveurGaussSeidel.ResultatConvergence resultat = get();
                        traiterResultatResolution(resultat);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(DifferencesFinis2DInterface.this,
                            "Erreur lors de la résolution: " + e.getMessage(),
                            "Erreur", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        setControlsEnabled(true);
                        barreProgres.setVisible(false);
                        labelStatut.setText("Résolution terminée");
                    }
                }
            };

        worker.execute();
    }

    /**
     * Traite les résultats de la résolution
     */
    private void traiterResultatResolution(SolveurGaussSeidel.ResultatConvergence resultat) {
        // Affichage des résultats
        StringBuilder sb = new StringBuilder();
        sb.append("=== RÉSULTATS DE RÉSOLUTION ===\n");
        sb.append("Méthode: ").append(comboMethode.getSelectedItem()).append("\n");
        sb.append("Convergé: ").append(resultat.converge ? "OUI" : "NON").append("\n");
        sb.append("Itérations: ").append(resultat.iterations).append("\n");
        sb.append("Erreur finale: ").append(String.format("%.2e", resultat.erreurFinale)).append("\n");
        sb.append("Temps de calcul: ").append(resultat.tempsCalcul).append(" ms\n");

        // Analyse des erreurs si solution exacte disponible
        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
        if (casTest == Maillage.CasTest.CAS1 || casTest == Maillage.CasTest.CAS2 || casTest == Maillage.CasTest.CAS5) {
            derniereAnalyse = AnalyseurErreurs.calculerErreurs(maillageActuel);

            sb.append("\n=== ANALYSE DES ERREURS ===\n");
            sb.append("Erreur L² (intégration numérique): ").append(String.format("%.6e", derniereAnalyse.erreurL2)).append("\n");
            sb.append("Erreur maximum: ").append(String.format("%.6e", derniereAnalyse.erreurMax)).append("\n");
            sb.append("Erreur moyenne: ").append(String.format("%.6e", derniereAnalyse.erreurMoyenne)).append("\n");
        }

        // Analyse de la convergence itérative
        double facteurConvergence = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
        sb.append("\nFacteur de convergence: ").append(String.format("%.4f", facteurConvergence)).append("\n");

        sb.append("\n");
        ajouterTexteResultats(sb.toString());

        // Mise à jour des visualisations
        panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
        panelContours.mettreAJour(maillageActuel, derniereAnalyse);
    }

    /**
     * Lance une analyse de convergence avec plusieurs maillages
     */
    private void analyserConvergence() {
        if (maillageActuel == null) {
            JOptionPane.showMessageDialog(this, "Veuillez d'abord configurer un cas de test.");
            return;
        }

        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();

        // Vérification que le cas a une solution exacte
        if (casTest != Maillage.CasTest.CAS1 && casTest != Maillage.CasTest.CAS2 && casTest != Maillage.CasTest.CAS5) {
            JOptionPane.showMessageDialog(this,
                "L'analyse de convergence nécessite un cas avec solution exacte connue.\n" +
                    "Utilisez CAS1, CAS2 ou CAS5.", "Information", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Analyse de convergence en cours...");

        SolveurGaussSeidel.MethodeResolution methode =
            (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();

        SwingWorker<AnalyseurErreurs.EtudeConvergence, Void> worker =
            new SwingWorker<AnalyseurErreurs.EtudeConvergence, Void>() {

                @Override
                protected AnalyseurErreurs.EtudeConvergence doInBackground() throws Exception {
                    int[] tailles = {10, 20, 30, 40, 50}; // Différentes tailles de maillage
                    return AnalyseurErreurs.etudierConvergence(casTest, tailles, methode);
                }

                @Override
                protected void done() {
                    try {
                        AnalyseurErreurs.EtudeConvergence etude = get();
                        afficherResultatsConvergence(etude);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(DifferencesFinis2DInterface.this,
                            "Erreur lors de l'analyse: " + e.getMessage());
                    } finally {
                        setControlsEnabled(true);
                        barreProgres.setVisible(false);
                        labelStatut.setText("Analyse terminée");
                    }
                }
            };

        worker.execute();
    }

    /**
     * Affiche les résultats de l'étude de convergence
     */
    private void afficherResultatsConvergence(AnalyseurErreurs.EtudeConvergence etude) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== ÉTUDE DE CONVERGENCE ===\n");
        sb.append("Cas: ").append(comboCasTest.getSelectedItem()).append("\n");
        sb.append("Méthode: ").append(comboMethode.getSelectedItem()).append("\n\n");

        sb.append("Ordre de convergence:\n");
        sb.append("  Norme L²: ").append(String.format("%.2f", etude.ordreConvergenceL2)).append("\n");
        sb.append("  Norme max: ").append(String.format("%.2f", etude.ordreConvergenceMax)).append("\n");
        sb.append("  Théorique: 2.0 (différences finies O(h²))\n\n");

        sb.append("Détail par maillage:\n");
        sb.append(String.format("%-10s %-12s %-12s\n", "h", "Erreur L²", "Erreur max"));
        sb.append("----------------------------------------\n");

        for (int i = 0; i < etude.taillesMaillage.length; i++) {
            sb.append(String.format("%-10.6f %-12.2e %-12.2e\n",
                etude.taillesMaillage[i], etude.erreursL2[i], etude.erreursMax[i]));
        }

        sb.append("\n");
        ajouterTexteResultats(sb.toString());

        // Fenêtre graphique pour visualiser la convergence
        creerGraphiqueConvergence(etude);
    }

    /**
     * Compare les performances des différentes méthodes
     */
    private void comparerMethodes() {
        if (maillageActuel == null) {
            configurerCasTest();
        }

        setControlsEnabled(false);
        labelStatut.setText("Comparaison des méthodes...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                SolveurGaussSeidel.MethodeResolution[] methodes = {
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_PARALLELE
                };

                publish("=== COMPARAISON DES MÉTHODES ===\n");
                publish("Maillage: " + (maillageActuel.getN() + 2) + "×" + (maillageActuel.getN() + 2) + "\n");
                publish("Cas: " + comboCasTest.getSelectedItem() + "\n\n");

                for (SolveurGaussSeidel.MethodeResolution methode : methodes) {
                    publish("Test de: " + methode.getNom() + "\n");

                    // Sauvegarde de l'état initial
                    double[][] solutionInitiale = maillageActuel.copierSolution();

                    double tolerance = (Double) spinnerTolerance.getValue();
                    int maxIter = (Integer) spinnerMaxIterations.getValue();
                    double omega = (methode == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                        (Double) spinnerOmega.getValue() : 1.0;

                    long debut = System.currentTimeMillis();
                    SolveurGaussSeidel.ResultatConvergence resultat =
                        solveur.resoudre(maillageActuel, methode, tolerance, maxIter, omega, null);
                    long fin = System.currentTimeMillis();

                    publish("  Temps: " + (fin - debut) + " ms\n");
                    publish("  Itérations: " + resultat.iterations + "\n");
                    publish("  Convergé: " + (resultat.converge ? "OUI" : "NON") + "\n");
                    publish("  Erreur finale: " + String.format("%.2e", resultat.erreurFinale) + "\n");

                    // Calcul de l'erreur par rapport à la solution exacte si disponible
                    Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
                    if (casTest == Maillage.CasTest.CAS1 || casTest == Maillage.CasTest.CAS2 || casTest == Maillage.CasTest.CAS5) {
                        AnalyseurErreurs.ResultatAnalyse analyse = AnalyseurErreurs.calculerErreurs(maillageActuel);
                        publish("  Erreur L²: " + String.format("%.6e", analyse.erreurL2) + "\n");
                    }

                    // Analyse de la convergence
                    double facteur = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
                    publish("  Facteur de convergence: " + String.format("%.4f", facteur) + "\n\n");

                    // Restauration pour le test suivant
                    maillageActuel.restaurerSolution(solutionInitiale);
                }

                return null;
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) {
                    ajouterTexteResultats(message);
                }
            }

            @Override
            protected void done() {
                setControlsEnabled(true);
                labelStatut.setText("Comparaison terminée");
            }
        };

        worker.execute();
    }

    /**
     * Remet l'interface à son état par défaut
     */
    private void resetToDefault() {
        // Réinitialisation des contrôles
        comboCasTest.setSelectedItem(Maillage.CasTest.CAS1);
        spinnerTailleMaillage.setValue(20);
        comboMethode.setSelectedItem(SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE);
        spinnerTolerance.setValue(1e-6);
        spinnerMaxIterations.setValue(5000);
        spinnerOmega.setValue(1.0);

        // Nettoyage des résultats
        zoneResultats.setText("");
        derniereAnalyse = null;

        // Configuration du cas par défaut
        configurerCasTest();

        labelStatut.setText("Interface réinitialisée");

        ajouterTexteResultats("=== SOLVEUR DIFFÉRENCES FINIES 2D ===\n");
        ajouterTexteResultats("Équation résolue: -ΔU = f\n");
        ajouterTexteResultats("Domaine: [0,1] × [0,1]\n");
        ajouterTexteResultats("Conditions aux limites: U = 0 sur le bord\n");
        ajouterTexteResultats("Discrétisation: Schéma 5 points\n\n");
    }

    /**
     * Exporte les résultats vers un fichier
     */
    private void exporterResultats() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers texte", "txt"));

        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new java.io.File(file.getAbsolutePath() + ".txt");
                }

                try (java.io.PrintWriter writer = new java.io.PrintWriter(file)) {
                    writer.println("=== RAPPORT DIFFÉRENCES FINIES 2D ===");
                    writer.println("Date: " + new java.util.Date());
                    writer.println();

                    // Configuration
                    writer.println("CONFIGURATION:");
                    writer.println("Cas de test: " + comboCasTest.getSelectedItem());
                    writer.println("Taille maillage: " + (maillageActuel.getN() + 2) + "×" + (maillageActuel.getN() + 2));
                    writer.println("Méthode: " + comboMethode.getSelectedItem());
                    writer.println("Tolérance: " + spinnerTolerance.getValue());
                    writer.println();

                    // Résultats détaillés
                    writer.println("RÉSULTATS DÉTAILLÉS:");
                    writer.println(zoneResultats.getText());

                    // Solution numérique (extrait)
                    if (maillageActuel != null) {
                        writer.println("\nSOLUTION NUMÉRIQUE (extrait):");
                        double[][] U = maillageActuel.getU();
                        int N = maillageActuel.getN();

                        writer.println("Points centraux du maillage:");
                        for (int i = N/2 - 2; i <= N/2 + 2; i++) {
                            for (int j = N/2 - 2; j <= N/2 + 2; j++) {
                                if (i >= 1 && i <= N && j >= 1 && j <= N) {
                                    writer.printf("U[%d,%d] = %.6f   ", i, j, U[i][j]);
                                }
                            }
                            writer.println();
                        }
                    }
                }

                JOptionPane.showMessageDialog(this, "Résultats exportés vers: " + file.getAbsolutePath());

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'export: " + e.getMessage());
            }
        }
    }

    /**
     * Crée un graphique de convergence
     */
    private void creerGraphiqueConvergence(AnalyseurErreurs.EtudeConvergence etude) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetreGraphique = new JFrame("Étude de Convergence");
            fenetreGraphique.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fenetreGraphique.setSize(600, 400);
            fenetreGraphique.setLocationRelativeTo(this);

            // Panel personnalisé pour dessiner le graphique
            JPanel panelGraphique = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    dessinerGraphiqueConvergence(g, etude);
                }
            };

            panelGraphique.setBackground(Color.WHITE);
            fenetreGraphique.add(panelGraphique);
            fenetreGraphique.setVisible(true);
        });
    }

    /**
     * Dessine le graphique de convergence
     */
    private void dessinerGraphiqueConvergence(Graphics g, AnalyseurErreurs.EtudeConvergence etude) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int largeur = getWidth() - 100;
        int hauteur = getHeight() - 100;
        int offsetX = 50;
        int offsetY = 50;

        // Axes
        g2d.setColor(Color.BLACK);
        g2d.drawLine(offsetX, offsetY + hauteur, offsetX + largeur, offsetY + hauteur); // Axe X
        g2d.drawLine(offsetX, offsetY, offsetX, offsetY + hauteur); // Axe Y

        // Échelles logarithmiques
        double minH = etude.taillesMaillage[etude.taillesMaillage.length - 1];
        double maxH = etude.taillesMaillage[0];
        double minErr = Math.min(
            java.util.Arrays.stream(etude.erreursL2).min().orElse(1e-10),
            java.util.Arrays.stream(etude.erreursMax).min().orElse(1e-10)
        );
        double maxErr = Math.max(
            java.util.Arrays.stream(etude.erreursL2).max().orElse(1e-2),
            java.util.Arrays.stream(etude.erreursMax).max().orElse(1e-2)
        );

        // Courbe erreur L²
        g2d.setColor(Color.BLUE);
        g2d.setStroke(new BasicStroke(2));
        for (int i = 0; i < etude.taillesMaillage.length - 1; i++) {
            int x1 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y1 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursL2[i]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            int x2 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i+1]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y2 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursL2[i+1]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Courbe erreur max
        g2d.setColor(Color.RED);
        for (int i = 0; i < etude.taillesMaillage.length - 1; i++) {
            int x1 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y1 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursMax[i]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            int x2 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i+1]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y2 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursMax[i+1]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Légende
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Convergence en fonction de h", offsetX + largeur/2 - 50, offsetY - 10);
        g2d.drawString("h", offsetX + largeur + 10, offsetY + hauteur + 5);
        g2d.drawString("Erreur", offsetX - 40, offsetY + 5);

        g2d.setColor(Color.BLUE);
        g2d.drawString("Erreur L²", offsetX + largeur - 150, offsetY + 30);
        g2d.setColor(Color.RED);
        g2d.drawString("Erreur max", offsetX + largeur - 150, offsetY + 50);
    }

    /**
     * Active/désactive les contrôles
     */
    private void setControlsEnabled(boolean enabled) {
        comboCasTest.setEnabled(enabled);
        spinnerTailleMaillage.setEnabled(enabled);
        comboMethode.setEnabled(enabled);
        spinnerTolerance.setEnabled(enabled);
        spinnerMaxIterations.setEnabled(enabled);
        spinnerOmega.setEnabled(enabled);
        boutonResoudre.setEnabled(enabled);
        boutonAnalyserConvergence.setEnabled(enabled);
        boutonComparerMethodes.setEnabled(enabled);
        boutonReset.setEnabled(enabled);
        boutonExporter.setEnabled(enabled);
    }

    /**
     * Ajoute du texte à la zone de résultats
     */
    private void ajouterTexteResultats(String texte) {
        zoneResultats.append(texte);
        zoneResultats.setCaretPosition(zoneResultats.getDocument().getLength());
    }

    /**
     * Nettoyage des ressources à la fermeture
     */
    @Override
    public void dispose() {
        if (solveur != null) {
            solveur.fermer();
        }
        super.dispose();
    }

    /**
     * Méthode principale pour tester l'interface
     */
    public static void main(String[] args) {
        // Configuration du Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Lancement de l'interface
        SwingUtilities.invokeLater(() -> {
            new DifferencesFinis2DInterface().setVisible(true);
        });
    }
}
