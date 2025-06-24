import javax.swing.*;
import java.awt.*;

/**
 * Interface utilisateur principale pour le solveur de différences finies 2D
 * avec conditions aux limites de Dirichlet générales
 *
 * Cette interface permet de :
 * - Configurer les paramètres du problème (cas de test, taille de maillage)
 * - Spécifier les conditions aux limites de Dirichlet générales
 * - Sélectionner la méthode de résolution (Gauss-Seidel classique, avec relaxation, parallélisé)
 * - Lancer les calculs et suivre leur progression
 * - Visualiser les résultats avec différents types d'affichage
 * - Analyser les erreurs et la convergence
 * - Comparer les performances des méthodes
 */
public class DifferencesFinis2DInterface extends JFrame {

    // Constantes d'interface
    private static final int LARGEUR_FENETRE = 1500;
    private static final int HAUTEUR_FENETRE = 1000;

    // Composants GUI principaux
    private JPanel panelControles;
    private JPanel panelVisualisations;
    private JTextArea zoneResultats;
    private JProgressBar barreProgres;
    private JLabel labelStatut;

    // Contrôles de configuration du problème
    private JComboBox<Maillage.CasTest> comboCasTest;
    private JSpinner spinnerTailleMaillage;
    private JComboBox<SolveurGaussSeidel.MethodeResolution> comboMethode;
    private JSpinner spinnerTolerance;
    private JSpinner spinnerMaxIterations;
    private JSpinner spinnerOmega;
    private JLabel labelOmega;

    // Contrôles des conditions aux limites
    private JComboBox<ConditionsLimites.TypeCondition> comboTypeConditions;
    private JSpinner spinnerValInferieure;
    private JSpinner spinnerValSuperieure;
    private JSpinner spinnerValGauche;
    private JSpinner spinnerValDroite;
    private JButton boutonConditionsAvancees;
    private JCheckBox checkConditionsPersonnalisees;

    // Boutons d'action
    private JButton boutonResoudre;
    private JButton boutonAnalyserConvergence;
    private JButton boutonComparerMethodes;
    private JButton boutonAnalyserConditions;
    private JButton boutonReset;
    private JButton boutonExporter;

    // Panels de visualisation
    private VisualiseurGraphique.PanelVisualisation panelColoration;
    private VisualiseurGraphique.PanelVisualisation panelContours;

    // Données du problème
    private Maillage maillageActuel;
    private SolveurGaussSeidel solveur;
    private AnalyseurErreurs.ResultatAnalyse derniereAnalyse;
    private ConditionsLimites conditionsActuelles;

    /**
     * Constructeur de l'interface principale
     */
    public DifferencesFinis2DInterface() {
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        resetToDefault();

        System.out.println("Interface utilisateur avec conditions générales initialisée");
    }

    /**
     * Initialise tous les composants GUI
     */
    private void initializeComponents() {
        setTitle("Solveur Différences Finies 2D - Équation -ΔU = f avec Conditions Générales");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(LARGEUR_FENETRE, HAUTEUR_FENETRE);
        setLocationRelativeTo(null);

        // Initialisation du solveur
        solveur = new SolveurGaussSeidel();
        conditionsActuelles = new ConditionsLimites(); // Conditions homogènes par défaut

        // === Panel de contrôles ===
        panelControles = new JPanel();
        panelControles.setBorder(BorderFactory.createTitledBorder("Configuration"));
        panelControles.setPreferredSize(new Dimension(400, 0));

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

        // === Contrôles des conditions aux limites ===
        comboTypeConditions = new JComboBox<>(ConditionsLimites.TypeCondition.values());
        comboTypeConditions.setToolTipText("Type de conditions aux limites");

        // Valeurs numériques pour les conditions constantes
        spinnerValInferieure = new JSpinner(new SpinnerNumberModel(0.0, -10.0, 10.0, 0.1));
        spinnerValSuperieure = new JSpinner(new SpinnerNumberModel(0.0, -10.0, 10.0, 0.1));
        spinnerValGauche = new JSpinner(new SpinnerNumberModel(0.0, -10.0, 10.0, 0.1));
        spinnerValDroite = new JSpinner(new SpinnerNumberModel(0.0, -10.0, 10.0, 0.1));

        boutonConditionsAvancees = new JButton("Conditions Avancées...");
        boutonConditionsAvancees.setToolTipText("Ouvrir l'éditeur de conditions aux limites avancées");

        checkConditionsPersonnalisees = new JCheckBox("Utiliser conditions personnalisées");

        // Boutons d'action
        boutonResoudre = new JButton("Résoudre");
        boutonResoudre.setFont(new Font("Arial", Font.BOLD, 12));
        boutonResoudre.setBackground(new Color(100, 150, 100));
        boutonResoudre.setForeground(Color.WHITE);

        boutonAnalyserConvergence = new JButton("Analyser Convergence");
        boutonComparerMethodes = new JButton("Comparer Méthodes");
        boutonAnalyserConditions = new JButton("Analyser Conditions");
        boutonReset = new JButton("Reset");
        boutonExporter = new JButton("Exporter Résultats");

        // Barre de progression et statut
        barreProgres = new JProgressBar();
        barreProgres.setStringPainted(true);
        barreProgres.setVisible(false);

        labelStatut = new JLabel("Prêt - Conditions homogènes");
        labelStatut.setBorder(BorderFactory.createEtchedBorder());

        // === Zone de résultats ===
        zoneResultats = new JTextArea();
        zoneResultats.setFont(new Font("Courier New", Font.PLAIN, 11));
        zoneResultats.setEditable(false);
        zoneResultats.setBackground(Color.BLACK);

        // === Panels de visualisation ===
        panelColoration = new VisualiseurGraphique.PanelVisualisation(450, 450);
        panelContours = new VisualiseurGraphique.PanelVisualisation(450, 450);

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

        // Section: Conditions aux limites
        JPanel sectionConditions = new JPanel(new GridBagLayout());
        sectionConditions.setBorder(BorderFactory.createTitledBorder("Conditions aux Limites"));

        gbc.gridx = 0; gbc.gridy = 0;
        sectionConditions.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        sectionConditions.add(comboTypeConditions, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        sectionConditions.add(new JLabel("Valeur inférieure (y=0):"), gbc);
        gbc.gridx = 1;
        sectionConditions.add(spinnerValInferieure, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        sectionConditions.add(new JLabel("Valeur supérieure (y=1):"), gbc);
        gbc.gridx = 1;
        sectionConditions.add(spinnerValSuperieure, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        sectionConditions.add(new JLabel("Valeur gauche (x=0):"), gbc);
        gbc.gridx = 1;
        sectionConditions.add(spinnerValGauche, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        sectionConditions.add(new JLabel("Valeur droite (x=1):"), gbc);
        gbc.gridx = 1;
        sectionConditions.add(spinnerValDroite, gbc);

        gbc.gridx = 0; gbc.gridy = 5; gbc.gridwidth = 2;
        sectionConditions.add(checkConditionsPersonnalisees, gbc);

        gbc.gridy = 6;
        sectionConditions.add(boutonConditionsAvancees, gbc);

        // Section: Méthode
        JPanel sectionMethode = new JPanel(new GridBagLayout());
        sectionMethode.setBorder(BorderFactory.createTitledBorder("Méthode de Résolution"));
        gbc.gridwidth = 1;

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
        JPanel sectionActions = new JPanel(new GridLayout(6, 1, 5, 5));
        sectionActions.setBorder(BorderFactory.createTitledBorder("Actions"));
        sectionActions.add(boutonResoudre);
        sectionActions.add(boutonAnalyserConvergence);
        sectionActions.add(boutonComparerMethodes);
        sectionActions.add(boutonAnalyserConditions);
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
        panelControles.add(sectionConditions);
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

        // Changement de type de conditions aux limites
        comboTypeConditions.addActionListener(e -> mettreAJourConditionsLimites());

        // Changement des valeurs numériques des conditions
        spinnerValInferieure.addChangeListener(e -> mettreAJourConditionsLimites());
        spinnerValSuperieure.addChangeListener(e -> mettreAJourConditionsLimites());
        spinnerValGauche.addChangeListener(e -> mettreAJourConditionsLimites());
        spinnerValDroite.addChangeListener(e -> mettreAJourConditionsLimites());

        // Changement de cas de test
        comboCasTest.addActionListener(e -> configurerCasTest());

        // Changement de taille de maillage
        spinnerTailleMaillage.addChangeListener(e -> configurerCasTest());

        // Conditions personnalisées
        checkConditionsPersonnalisees.addActionListener(e -> {
            boolean personnalisees = checkConditionsPersonnalisees.isSelected();
            activerControlesConditions(!personnalisees);
            if (!personnalisees) {
                mettreAJourConditionsLimites();
            }
        });

        // Boutons d'action
        boutonResoudre.addActionListener(e -> resoudreProbleme());
        boutonAnalyserConvergence.addActionListener(e -> analyserConvergence());
        boutonComparerMethodes.addActionListener(e -> comparerMethodes());
        boutonAnalyserConditions.addActionListener(e -> analyserInfluenceConditions());
        boutonConditionsAvancees.addActionListener(e -> ouvrirEditeurConditions());
        boutonReset.addActionListener(e -> resetToDefault());
        boutonExporter.addActionListener(e -> exporterResultats());
    }

    /**
     * Met à jour les conditions aux limites selon l'interface
     */
    private void mettreAJourConditionsLimites() {
        if (checkConditionsPersonnalisees.isSelected()) {
            return; // Ne pas modifier les conditions personnalisées
        }

        ConditionsLimites.TypeCondition type = (ConditionsLimites.TypeCondition) comboTypeConditions.getSelectedItem();

        switch (type) {
            case HOMOGENES:
                conditionsActuelles = new ConditionsLimites();
                break;

            case CONSTANTES:
                double valInf = (Double) spinnerValInferieure.getValue();
                double valSup = (Double) spinnerValSuperieure.getValue();
                double valGauche = (Double) spinnerValGauche.getValue();
                double valDroite = (Double) spinnerValDroite.getValue();
                conditionsActuelles = new ConditionsLimites(valInf, valSup, valGauche, valDroite);
                break;

            case LINEAIRES:
                conditionsActuelles = ConditionsLimites.creerConditionsTest("lineaires");
                break;

            case SINUSOIDALES:
                conditionsActuelles = ConditionsLimites.creerConditionsTest("sinusoidales");
                break;

            case POLYNOMIALES:
                conditionsActuelles = ConditionsLimites.creerConditionsTest("polynomiales");
                break;

            default:
                conditionsActuelles = new ConditionsLimites();
        }

        // Vérifier la compatibilité
        if (!conditionsActuelles.verifierCompatibilite()) {
            ajouterTexteResultats("Attention: Conditions aux limites incompatibles aux coins!\n");
        }

        // Reconfigurer le maillage si il existe
        if (maillageActuel != null) {
            maillageActuel.setConditionsLimites(conditionsActuelles);
            panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
            panelContours.mettreAJour(maillageActuel, derniereAnalyse);
        }

        labelStatut.setText("Conditions: " + conditionsActuelles.getDescription());
    }

    /**
     * Active/désactive les contrôles de conditions aux limites
     */
    private void activerControlesConditions(boolean activer) {
        comboTypeConditions.setEnabled(activer);
        spinnerValInferieure.setEnabled(activer &&
            comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES);
        spinnerValSuperieure.setEnabled(activer &&
            comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES);
        spinnerValGauche.setEnabled(activer &&
            comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES);
        spinnerValDroite.setEnabled(activer &&
            comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES);
    }

    /**
     * Ouvre l'éditeur avancé de conditions aux limites
     */
    private void ouvrirEditeurConditions() {
        JDialog dialogue = new JDialog(this, "Éditeur de Conditions aux Limites", true);
        dialogue.setSize(600, 400);
        dialogue.setLocationRelativeTo(this);

        JPanel panelPrincipal = new JPanel(new BorderLayout());

        // Zone de texte pour la description
        JTextArea descriptionArea = new JTextArea(conditionsActuelles.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setBorder(BorderFactory.createTitledBorder("Description Actuelle"));

        // Boutons prédéfinis
        JPanel panelBoutons = new JPanel(new GridLayout(3, 2, 5, 5));
        panelBoutons.setBorder(BorderFactory.createTitledBorder("Conditions Prédéfinies"));

        String[] typesPredefinis = {"homogenes", "constantes_unitaires", "constantes_variables",
            "lineaires", "sinusoidales", "polynomiales"};

        for (String type : typesPredefinis) {
            JButton bouton = new JButton(type.replace("_", " "));
            bouton.addActionListener(e -> {
                conditionsActuelles = ConditionsLimites.creerConditionsTest(type);
                descriptionArea.setText(conditionsActuelles.getDescription());
                checkConditionsPersonnalisees.setSelected(true);
                activerControlesConditions(false);
            });
            panelBoutons.add(bouton);
        }

        // Boutons de validation
        JPanel panelValidation = new JPanel(new FlowLayout());
        JButton boutonOK = new JButton("Appliquer");
        JButton boutonAnnuler = new JButton("Annuler");

        boutonOK.addActionListener(e -> {
            if (maillageActuel != null) {
                maillageActuel.setConditionsLimites(conditionsActuelles);
                panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
                panelContours.mettreAJour(maillageActuel, derniereAnalyse);
            }
            labelStatut.setText("Conditions: " + conditionsActuelles.getDescription());
            dialogue.dispose();
        });

        boutonAnnuler.addActionListener(e -> dialogue.dispose());

        panelValidation.add(boutonOK);
        panelValidation.add(boutonAnnuler);

        panelPrincipal.add(descriptionArea, BorderLayout.NORTH);
        panelPrincipal.add(panelBoutons, BorderLayout.CENTER);
        panelPrincipal.add(panelValidation, BorderLayout.SOUTH);

        dialogue.add(panelPrincipal);
        dialogue.setVisible(true);
    }

    /**
     * Configure le cas de test sélectionné avec les conditions actuelles
     */
    private void configurerCasTest() {
        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
        int N = (Integer) spinnerTailleMaillage.getValue();

        if (maillageActuel == null || maillageActuel.getN() != N) {
            maillageActuel = new Maillage(N, conditionsActuelles);
        } else {
            maillageActuel.setConditionsLimites(conditionsActuelles);
        }

        maillageActuel.configurerCasTest(casTest, conditionsActuelles);

        // Mise à jour de l'affichage
        panelColoration.mettreAJour(maillageActuel, null);
        panelContours.mettreAJour(maillageActuel, null);

        ajouterTexteResultats("Configuration: " + casTest.getDescription() +
            " sur maillage " + (N+2) + "×" + (N+2) + "\n");
        ajouterTexteResultats("Conditions aux limites: " + conditionsActuelles.getDescription() + "\n");

        // Calcul du ω optimal pour SOR
        if (comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) {
            double omegaOptimal = SolveurGaussSeidel.calculerOmegaOptimal(N);
            spinnerOmega.setValue(omegaOptimal);
        }

        labelStatut.setText("Cas configuré: " + casTest.name() + " avec " + conditionsActuelles.getType().name());
    }

    /**
     * Lance la résolution du problème avec les conditions aux limites actuelles
     */
    private void resoudreProbleme() {
        if (maillageActuel == null) {
            configurerCasTest();
        }

        // Vérification de la cohérence
        if (!maillageActuel.verifierCoherence()) {
            JOptionPane.showMessageDialog(this,
                "Attention: Le maillage présente des incohérences!\nVérifiez les conditions aux limites.",
                "Avertissement", JOptionPane.WARNING_MESSAGE);
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
                    publish("Début de la résolution avec conditions générales...");

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
     * Traite les résultats de la résolution avec conditions générales
     */
    private void traiterResultatResolution(SolveurGaussSeidel.ResultatConvergence resultat) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== RÉSULTATS DE RÉSOLUTION ===\n");
        sb.append("Méthode: ").append(comboMethode.getSelectedItem()).append("\n");
        sb.append("Conditions aux limites: ").append(conditionsActuelles.getDescription()).append("\n");
        sb.append("Convergé: ").append(resultat.converge ? "OUI" : "NON").append("\n");
        sb.append("Itérations: ").append(resultat.iterations).append("\n");
        sb.append("Erreur finale: ").append(String.format("%.2e", resultat.erreurFinale)).append("\n");
        sb.append("Temps de calcul: ").append(resultat.tempsCalcul).append(" ms\n");

        // Calcul du résidu
        double residu = solveur.calculerResidu(maillageActuel);
        sb.append("Résidu de l'équation: ").append(String.format("%.2e", residu)).append("\n");

        // Analyse des erreurs (si solution exacte disponible)
        derniereAnalyse = AnalyseurErreurs.calculerErreurs(maillageActuel);

        if (derniereAnalyse.solutionExacteDisponible) {
            sb.append("\n=== ANALYSE DES ERREURS ===\n");
            sb.append("Erreur L² (intégration numérique): ").append(String.format("%.6e", derniereAnalyse.erreurL2)).append("\n");
            sb.append("Erreur maximum: ").append(String.format("%.6e", derniereAnalyse.erreurMax)).append("\n");
            sb.append("Erreur moyenne: ").append(String.format("%.6e", derniereAnalyse.erreurMoyenne)).append("\n");
        } else {
            sb.append("\nSolution exacte non disponible pour ce cas avec ces conditions.\n");
        }

        // Analyse de la convergence itérative
        double facteurConvergence = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
        if (!Double.isNaN(facteurConvergence)) {
            sb.append("Facteur de convergence: ").append(String.format("%.4f", facteurConvergence)).append("\n");
        }

        sb.append("\n");
        ajouterTexteResultats(sb.toString());

        // Mise à jour des visualisations
        panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
        panelContours.mettreAJour(maillageActuel, derniereAnalyse);

        // Analyse rapide complémentaire
        AnalyseurErreurs.analyseRapide(maillageActuel, resultat);
    }

    /**
     * Lance une analyse de convergence avec les conditions actuelles
     */
    private void analyserConvergence() {
        if (maillageActuel == null) {
            JOptionPane.showMessageDialog(this, "Veuillez d'abord configurer un cas de test.");
            return;
        }

        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
        SolveurGaussSeidel.MethodeResolution methode =
            (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();

        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Analyse de convergence en cours...");

        SwingWorker<AnalyseurErreurs.EtudeConvergence, Void> worker =
            new SwingWorker<AnalyseurErreurs.EtudeConvergence, Void>() {

                @Override
                protected AnalyseurErreurs.EtudeConvergence doInBackground() throws Exception {
                    int[] tailles = {10, 15, 20, 30, 40}; // Différentes tailles de maillage
                    return AnalyseurErreurs.etudierConvergence(casTest, tailles, methode, conditionsActuelles);
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
        sb.append("=== ÉTUDE DE CONVERGENCE AVEC CONDITIONS GÉNÉRALES ===\n");
        sb.append("Cas: ").append(comboCasTest.getSelectedItem()).append("\n");
        sb.append("Méthode: ").append(comboMethode.getSelectedItem()).append("\n");
        sb.append("Conditions: ").append(conditionsActuelles.getDescription()).append("\n\n");

        sb.append("Ordre de convergence:\n");
        sb.append("  Norme L²: ").append(String.format("%.2f", etude.ordreConvergenceL2)).append("\n");
        sb.append("  Norme max: ").append(String.format("%.2f", etude.ordreConvergenceMax)).append("\n");
        sb.append("  Théorique: 2.0 (différences finies O(h²))\n\n");

        sb.append("Détail par maillage:\n");
        sb.append(String.format("%-10s %-12s %-12s %-12s\n", "h", "Erreur L²", "Erreur max", "Err. disc."));
        sb.append("----------------------------------------------------\n");

        for (int i = 0; i < etude.taillesMaillage.length; i++) {
            sb.append(String.format("%-10.6f %-12.2e %-12.2e %-12.2e\n",
                etude.taillesMaillage[i], etude.erreursL2[i], etude.erreursMax[i], etude.erreursDiscretes[i]));
        }

        sb.append("\n");
        ajouterTexteResultats(sb.toString());

        // Fenêtre graphique pour visualiser la convergence
        creerGraphiqueConvergence(etude);
    }

    /**
     * Analyse l'influence des conditions aux limites
     */
    private void analyserInfluenceConditions() {
        if (maillageActuel == null) {
            configurerCasTest();
        }

        setControlsEnabled(false);
        labelStatut.setText("Analyse de l'influence des conditions aux limites...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
                int N = (Integer) spinnerTailleMaillage.getValue();
                SolveurGaussSeidel.MethodeResolution methode =
                    (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();

                publish("=== ANALYSE DE L'INFLUENCE DES CONDITIONS AUX LIMITES ===\n");

                AnalyseurErreurs.analyserInfluenceConditionsLimites(casTest, N, methode);

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
                labelStatut.setText("Analyse des conditions terminée");
            }
        };

        worker.execute();
    }

    /**
     * Compare les performances des différentes méthodes avec conditions actuelles
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
                publish("=== COMPARAISON DES MÉTHODES AVEC CONDITIONS GÉNÉRALES ===\n");
                publish("Conditions aux limites: " + conditionsActuelles.getDescription() + "\n");
                publish("Maillage: " + (maillageActuel.getN() + 2) + "×" + (maillageActuel.getN() + 2) + "\n\n");

                SolveurGaussSeidel.MethodeResolution[] methodes = {
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_PARALLELE
                };

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

                    // Calcul du résidu
                    double residu = solveur.calculerResidu(maillageActuel);
                    publish("  Résidu: " + String.format("%.2e", residu) + "\n");

                    // Calcul de l'erreur par rapport à la solution exacte si disponible
                    AnalyseurErreurs.ResultatAnalyse analyse = AnalyseurErreurs.calculerErreurs(maillageActuel);
                    if (analyse.solutionExacteDisponible) {
                        publish("  Erreur L²: " + String.format("%.6e", analyse.erreurL2) + "\n");
                    }

                    // Analyse de la convergence
                    double facteur = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
                    if (!Double.isNaN(facteur)) {
                        publish("  Facteur de convergence: " + String.format("%.4f", facteur) + "\n");
                    }
                    publish("\n");

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
        // Réinitialisation des contrôles du problème
        comboCasTest.setSelectedItem(Maillage.CasTest.CAS1);
        spinnerTailleMaillage.setValue(20);
        comboMethode.setSelectedItem(SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE);
        spinnerTolerance.setValue(1e-6);
        spinnerMaxIterations.setValue(5000);
        spinnerOmega.setValue(1.0);

        // Réinitialisation des conditions aux limites
        comboTypeConditions.setSelectedItem(ConditionsLimites.TypeCondition.HOMOGENES);
        spinnerValInferieure.setValue(0.0);
        spinnerValSuperieure.setValue(0.0);
        spinnerValGauche.setValue(0.0);
        spinnerValDroite.setValue(0.0);
        checkConditionsPersonnalisees.setSelected(false);
        activerControlesConditions(true);

        // Réinitialisation des conditions
        conditionsActuelles = new ConditionsLimites();

        // Nettoyage des résultats
        zoneResultats.setText("");
        derniereAnalyse = null;

        // Configuration du cas par défaut
        configurerCasTest();

        labelStatut.setText("Interface réinitialisée - Conditions homogènes");

        ajouterTexteResultats("=== SOLVEUR DIFFÉRENCES FINIES 2D AVEC CONDITIONS GÉNÉRALES ===\n");
        ajouterTexteResultats("Équation résolue: -ΔU = f\n");
        ajouterTexteResultats("Domaine: [0,1] × [0,1]\n");
        ajouterTexteResultats("Conditions aux limites: Dirichlet générales U(bord) = valeurs spécifiées\n");
        ajouterTexteResultats("Discrétisation: Schéma 5 points\n\n");
    }

    /**
     * Exporte les résultats vers un fichier avec informations sur les conditions
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
                    writer.println("=== RAPPORT DIFFÉRENCES FINIES 2D AVEC CONDITIONS GÉNÉRALES ===");
                    writer.println("Date: " + new java.util.Date());
                    writer.println();

                    // Configuration
                    writer.println("CONFIGURATION:");
                    writer.println("Cas de test: " + comboCasTest.getSelectedItem());
                    writer.println("Taille maillage: " + (maillageActuel.getN() + 2) + "×" + (maillageActuel.getN() + 2));
                    writer.println("Méthode: " + comboMethode.getSelectedItem());
                    writer.println("Tolérance: " + spinnerTolerance.getValue());
                    writer.println();

                    // Conditions aux limites
                    writer.println("CONDITIONS AUX LIMITES:");
                    writer.println("Type: " + conditionsActuelles.getDescription());
                    writer.println("Compatibilité: " + (conditionsActuelles.verifierCompatibilite() ? "OK" : "Problème"));
                    if (conditionsActuelles.getType() == ConditionsLimites.TypeCondition.CONSTANTES) {
                        writer.println("Valeurs:");
                        writer.println("  Inférieure (y=0): " + conditionsActuelles.getValeurInferieure());
                        writer.println("  Supérieure (y=1): " + conditionsActuelles.getValeurSuperieure());
                        writer.println("  Gauche (x=0): " + conditionsActuelles.getValeurGauche());
                        writer.println("  Droite (x=1): " + conditionsActuelles.getValeurDroite());
                    }
                    writer.println();

                    // Résultats détaillés
                    writer.println("RÉSULTATS DÉTAILLÉS:");
                    writer.println(zoneResultats.getText());

                    // Solution numérique (extrait)
                    if (maillageActuel != null) {
                        writer.println("\nSOLUTION NUMÉRIQUE (extrait):");
                        double[][] U = maillageActuel.getU();
                        int N = maillageActuel.getN();

                        writer.println("Valeurs aux coins du domaine:");
                        writer.println("  U(0,0) = " + String.format("%.6f", U[0][0]));
                        writer.println("  U(1,0) = " + String.format("%.6f", U[0][N+1]));
                        writer.println("  U(0,1) = " + String.format("%.6f", U[N+1][0]));
                        writer.println("  U(1,1) = " + String.format("%.6f", U[N+1][N+1]));

                        writer.println("\nPoints centraux du maillage:");
                        for (int i = N/2 - 2; i <= N/2 + 2; i++) {
                            for (int j = N/2 - 2; j <= N/2 + 2; j++) {
                                if (i >= 1 && i <= N && j >= 1 && j <= N) {
                                    writer.printf("U[%d,%d] = %.6f   ", i, j, U[i][j]);
                                }
                            }
                            writer.println();
                        }
                    }

                    // Rapport complet d'analyse si disponible
                    if (derniereAnalyse != null && maillageActuel != null) {
                        writer.println("\n" + AnalyseurErreurs.genererRapportComplet(derniereAnalyse, null, maillageActuel));
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
            JFrame fenetreGraphique = new JFrame("Étude de Convergence - " + conditionsActuelles.getType().name());
            fenetreGraphique.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fenetreGraphique.setSize(700, 500);
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

            // Panel d'information
            JPanel panelInfo = new JPanel(new BorderLayout());
            JLabel labelInfo = new JLabel("<html><b>Conditions aux limites:</b> " +
                conditionsActuelles.getDescription() + "</html>");
            labelInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            panelInfo.add(labelInfo, BorderLayout.NORTH);
            panelInfo.add(panelGraphique, BorderLayout.CENTER);

            fenetreGraphique.add(panelInfo);
            fenetreGraphique.setVisible(true);
        });
    }

    /**
     * Dessine le graphique de convergence avec informations sur les conditions
     */
    private void dessinerGraphiqueConvergence(Graphics g, AnalyseurErreurs.EtudeConvergence etude) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int largeur = getWidth() - 120;
        int hauteur = getHeight() - 120;
        int offsetX = 60;
        int offsetY = 60;

        // Titre
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String titre = "Convergence avec " + conditionsActuelles.getType().name();
        g2d.drawString(titre, offsetX, offsetY - 20);

        // Axes
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
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
        g2d.setStroke(new BasicStroke(3));
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

        // Courbe erreur de discrétisation
        g2d.setColor(Color.GREEN);
        g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));
        for (int i = 0; i < etude.taillesMaillage.length - 1; i++) {
            int x1 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y1 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursDiscretes[i]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            int x2 = offsetX + (int) (largeur * (Math.log(etude.taillesMaillage[i+1]) - Math.log(maxH)) /
                (Math.log(minH) - Math.log(maxH)));
            int y2 = offsetY + hauteur - (int) (hauteur * (Math.log(etude.erreursDiscretes[i+1]) - Math.log(minErr)) /
                (Math.log(maxErr) - Math.log(minErr)));
            g2d.drawLine(x1, y1, x2, y2);
        }

        // Légende
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("h", offsetX + largeur + 10, offsetY + hauteur + 5);
        g2d.drawString("Erreur", offsetX - 50, offsetY + 5);

        // Légende des courbes
        g2d.setColor(Color.BLUE);
        g2d.drawString("Erreur L²", offsetX + largeur - 200, offsetY + 30);
        g2d.setColor(Color.RED);
        g2d.drawString("Erreur max", offsetX + largeur - 200, offsetY + 50);
        g2d.setColor(Color.GREEN);
        g2d.drawString("Erreur discrétisation", offsetX + largeur - 200, offsetY + 70);

        // Ordres de convergence
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 10));
        g2d.drawString("Ordre L²: " + String.format("%.2f", etude.ordreConvergenceL2), offsetX + largeur - 200, offsetY + 100);
        g2d.drawString("Ordre max: " + String.format("%.2f", etude.ordreConvergenceMax), offsetX + largeur - 200, offsetY + 115);
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

        // Contrôles des conditions aux limites
        comboTypeConditions.setEnabled(enabled && !checkConditionsPersonnalisees.isSelected());
        spinnerValInferieure.setEnabled(enabled && !checkConditionsPersonnalisees.isSelected());
        spinnerValSuperieure.setEnabled(enabled && !checkConditionsPersonnalisees.isSelected());
        spinnerValGauche.setEnabled(enabled && !checkConditionsPersonnalisees.isSelected());
        spinnerValDroite.setEnabled(enabled && !checkConditionsPersonnalisees.isSelected());
        boutonConditionsAvancees.setEnabled(enabled);
        checkConditionsPersonnalisees.setEnabled(enabled);

        // Boutons d'action
        boutonResoudre.setEnabled(enabled);
        boutonAnalyserConvergence.setEnabled(enabled);
        boutonComparerMethodes.setEnabled(enabled);
        boutonAnalyserConditions.setEnabled(enabled);
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
