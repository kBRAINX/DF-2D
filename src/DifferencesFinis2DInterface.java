import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Arrays;
import java.util.Collections; // Pour la ligne de séparation dans le tableau

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
    private JSpinner spinnerTailleMaillage; // N_total
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
        System.out.println("Interface utilisateur avec conditions générales initialisée (N est N_total)");
    }

    /**
     * Initialise tous les composants GUI
     */
    private void initializeComponents() {
        setTitle("Solveur Différences Finies 2D - Équation -ΔU = f avec Conditions Générales (N est N_total)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(LARGEUR_FENETRE, HAUTEUR_FENETRE);
        setLocationRelativeTo(null);

        solveur = new SolveurGaussSeidel();
        conditionsActuelles = new ConditionsLimites();

        panelControles = new JPanel();
        panelControles.setBorder(BorderFactory.createTitledBorder("Configuration"));
        panelControles.setPreferredSize(new Dimension(400, 0));

        comboCasTest = new JComboBox<>(Maillage.CasTest.values());
        comboCasTest.setToolTipText("Sélectionnez le cas de test à résoudre");

        // N_total doit être au moins 3 pour avoir N_interieur >= 1
        spinnerTailleMaillage = new JSpinner(new SpinnerNumberModel(20, 3, 1000, 1));
        spinnerTailleMaillage.setToolTipText("Nombre total de points par direction (N_total, bords inclus)");

        comboMethode = new JComboBox<>(SolveurGaussSeidel.MethodeResolution.values());
        comboMethode.setToolTipText("Méthode de résolution du système linéaire");

        spinnerTolerance = new JSpinner(new SpinnerNumberModel(1e-6, 1e-12, 1e-3, 1e-7)); // Précision du step
        spinnerTolerance.setEditor(new JSpinner.NumberEditor(spinnerTolerance, "0.#######E0"));

        spinnerMaxIterations = new JSpinner(new SpinnerNumberModel(5000, 100, 100000, 500));

        spinnerOmega = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 1.99, 0.05)); // Précision du step
        spinnerOmega.setEditor(new JSpinner.NumberEditor(spinnerOmega, "0.00"));
        labelOmega = new JLabel("Paramètre ω (relaxation):");

        comboTypeConditions = new JComboBox<>(ConditionsLimites.TypeCondition.values());
        comboTypeConditions.setToolTipText("Type de conditions aux limites");

        spinnerValInferieure = createBoundarySpinner();
        spinnerValSuperieure = createBoundarySpinner();
        spinnerValGauche = createBoundarySpinner();
        spinnerValDroite = createBoundarySpinner();

        boutonConditionsAvancees = new JButton("Conditions Avancées...");
        boutonConditionsAvancees.setToolTipText("Ouvrir l'éditeur de conditions aux limites avancées");
        checkConditionsPersonnalisees = new JCheckBox("Utiliser CL personnalisées");

        boutonResoudre = new JButton("Résoudre");
        boutonResoudre.setFont(new Font("Arial", Font.BOLD, 12));
        boutonResoudre.setBackground(new Color(100, 180, 100)); // Un peu plus clair
        boutonResoudre.setForeground(Color.WHITE);

        boutonAnalyserConvergence = new JButton("Analyser Convergence");
        boutonComparerMethodes = new JButton("Comparer Méthodes");
        boutonAnalyserConditions = new JButton("Analyser Influence CL");
        boutonReset = new JButton("Reset Config");
        boutonExporter = new JButton("Exporter Rapport");

        barreProgres = new JProgressBar();
        barreProgres.setStringPainted(true);
        barreProgres.setVisible(false);

        labelStatut = new JLabel("Prêt - Conditions homogènes par défaut");
        labelStatut.setBorder(BorderFactory.createEtchedBorder());
        labelStatut.setHorizontalAlignment(SwingConstants.CENTER);


        zoneResultats = new JTextArea();
        zoneResultats.setFont(new Font("Monospaced", Font.PLAIN, 12)); // Police Monospaced lisible
        zoneResultats.setEditable(false);
        zoneResultats.setBackground(Color.WHITE); // Fond blanc
        zoneResultats.setForeground(Color.BLACK); // Texte noir
        zoneResultats.setLineWrap(true);
        zoneResultats.setWrapStyleWord(true);


        panelColoration = new VisualiseurGraphique.PanelVisualisation(450, 450);
        panelContours = new VisualiseurGraphique.PanelVisualisation(450, 450);
        panelVisualisations = new JPanel(new GridLayout(1, 2, 10, 10));
        panelVisualisations.setBorder(BorderFactory.createTitledBorder("Visualisations"));

        panelColoration.setTypeVisualisation(VisualiseurGraphique.TypeVisualisation.COLORATION);
        panelContours.setTypeVisualisation(VisualiseurGraphique.TypeVisualisation.COURBES_NIVEAU);
    }

    private JSpinner createBoundarySpinner() {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(0.0, -100.0, 100.0, 0.1));
        spinner.setEditor(new JSpinner.NumberEditor(spinner, "0.0#"));
        return spinner;
    }


    /**
     * Configure la disposition des composants
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        panelControles.setLayout(new BoxLayout(panelControles, BoxLayout.Y_AXIS));

        // Section Problème
        JPanel sectionProbleme = createSectionPanel("Problème");
        addGridRow(sectionProbleme, new JLabel("Cas de test:"), comboCasTest);
        addGridRow(sectionProbleme, new JLabel("Taille maillage (N_total):"), spinnerTailleMaillage);

        // Section Conditions aux limites
        JPanel sectionConditions = createSectionPanel("Conditions aux Limites");
        addGridRow(sectionConditions, new JLabel("Type:"), comboTypeConditions);
        addGridRow(sectionConditions, new JLabel("Val. Inférieure (y=0):"), spinnerValInferieure);
        addGridRow(sectionConditions, new JLabel("Val. Supérieure (y=1):"), spinnerValSuperieure);
        addGridRow(sectionConditions, new JLabel("Val. Gauche (x=0):"), spinnerValGauche);
        addGridRow(sectionConditions, new JLabel("Val. Droite (x=1):"), spinnerValDroite);
        GridBagConstraints gbcFull = new GridBagConstraints();
        gbcFull.gridx = 0; gbcFull.gridy = 5; gbcFull.gridwidth = 2; gbcFull.anchor = GridBagConstraints.WEST; gbcFull.insets = new Insets(2,5,2,5);
        sectionConditions.add(checkConditionsPersonnalisees, gbcFull);
        gbcFull.gridy = 6;
        sectionConditions.add(boutonConditionsAvancees, gbcFull);

        // Section Méthode
        JPanel sectionMethode = createSectionPanel("Méthode de Résolution");
        addGridRow(sectionMethode, new JLabel("Méthode:"), comboMethode);
        addGridRow(sectionMethode, new JLabel("Tolérance:"), spinnerTolerance);
        addGridRow(sectionMethode, new JLabel("Max itérations:"), spinnerMaxIterations);
        addGridRow(sectionMethode, labelOmega, spinnerOmega);

        // Section Actions
        JPanel sectionActions = new JPanel(new GridLayout(0, 1, 5, 5)); // 0 lignes = autant que nécessaire
        sectionActions.setBorder(BorderFactory.createTitledBorder("Actions"));
        sectionActions.add(boutonResoudre);
        sectionActions.add(boutonAnalyserConvergence);
        sectionActions.add(boutonComparerMethodes);
        sectionActions.add(boutonAnalyserConditions);
        sectionActions.add(boutonReset);
        sectionActions.add(boutonExporter);

        // Section Progression
        JPanel sectionProgression = new JPanel(new BorderLayout(5,5));
        sectionProgression.setBorder(BorderFactory.createTitledBorder("Progression"));
        sectionProgression.add(barreProgres, BorderLayout.CENTER);
        sectionProgression.add(labelStatut, BorderLayout.SOUTH);

        panelControles.add(sectionProbleme);
        panelControles.add(createSmallVerticalStrut());
        panelControles.add(sectionConditions);
        panelControles.add(createSmallVerticalStrut());
        panelControles.add(sectionMethode);
        panelControles.add(createSmallVerticalStrut());
        panelControles.add(sectionActions);
        panelControles.add(createSmallVerticalStrut());
        panelControles.add(sectionProgression);
        panelControles.add(Box.createVerticalGlue()); // Pousse tout vers le haut

        // Visualisations
        JPanel panelColAvecTitre = new JPanel(new BorderLayout());
        panelColAvecTitre.add(new JLabel("Coloration (Heatmap)", JLabel.CENTER), BorderLayout.NORTH);
        panelColAvecTitre.add(panelColoration, BorderLayout.CENTER);

        JPanel panelContAvecTitre = new JPanel(new BorderLayout());
        panelContAvecTitre.add(new JLabel("Courbes de Niveau", JLabel.CENTER), BorderLayout.NORTH);
        panelContAvecTitre.add(panelContours, BorderLayout.CENTER);

        panelVisualisations.add(panelColAvecTitre);
        panelVisualisations.add(panelContAvecTitre);

        // Layout principal
        add(new JScrollPane(panelControles), BorderLayout.WEST); // Scrollable controls
        add(panelVisualisations, BorderLayout.CENTER);

        JTabbedPane onglets = new JTabbedPane();
        JScrollPane scrollPaneResultats = new JScrollPane(zoneResultats);
        scrollPaneResultats.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        onglets.addTab("Résultats Détaillés / Console", scrollPaneResultats);
        onglets.setPreferredSize(new Dimension(0, 300)); // Augmenter la hauteur de la zone de résultats
        add(onglets, BorderLayout.SOUTH);
    }

    private JPanel createSectionPanel(String title) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private void addGridRow(JPanel panel, Component comp1, Component comp2) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5); // top, left, bottom, right
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridy = panel.getComponentCount() / 2; // Approximatif pour gbc.gridy

        gbc.gridx = 0;
        panel.add(comp1, gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(comp2, gbc);
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.0;
    }

    private Component createSmallVerticalStrut() {
        return Box.createRigidArea(new Dimension(0, 5));
    }


    /**
     * Configure les gestionnaires d'événements
     */
    private void setupEventHandlers() {
        comboMethode.addActionListener(e -> {
            boolean isRelaxation = comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION;
            labelOmega.setVisible(isRelaxation);
            spinnerOmega.setVisible(isRelaxation);
            // panelControles.revalidate(); // Peut causer des sauts, pas toujours nécessaire si layout bien géré
        });

        comboTypeConditions.addActionListener(e -> mettreAJourInterfaceSelonTypeCL());

        // Listeners pour les spinners de CL, seulement si le type est CONSTANTES
        spinnerValInferieure.addChangeListener(e -> { if (comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES) mettreAJourConditionsLimites(); });
        spinnerValSuperieure.addChangeListener(e -> { if (comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES) mettreAJourConditionsLimites(); });
        spinnerValGauche.addChangeListener(e -> { if (comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES) mettreAJourConditionsLimites(); });
        spinnerValDroite.addChangeListener(e -> { if (comboTypeConditions.getSelectedItem() == ConditionsLimites.TypeCondition.CONSTANTES) mettreAJourConditionsLimites(); });


        comboCasTest.addActionListener(e -> configurerCasTest());
        spinnerTailleMaillage.addChangeListener(e -> configurerCasTest()); // Reconfigurer si taille change

        checkConditionsPersonnalisees.addActionListener(e -> {
            boolean personnalisees = checkConditionsPersonnalisees.isSelected();
            activerControlesStandardsCL(!personnalisees); // Désactive si perso
            if (!personnalisees) { // Si on décoche "perso", on remet à jour avec les CL standards
                mettreAJourInterfaceSelonTypeCL(); // Va appeler mettreAJourConditionsLimites
            }
            // Si on coche "perso", on ne fait rien, on attend l'éditeur ou on garde les CL perso actuelles.
        });

        boutonResoudre.addActionListener(e -> resoudreProbleme());
        boutonAnalyserConvergence.addActionListener(e -> analyserConvergence());
        boutonComparerMethodes.addActionListener(e -> comparerMethodes());
        boutonAnalyserConditions.addActionListener(e -> analyserInfluenceConditions());
        boutonConditionsAvancees.addActionListener(e -> ouvrirEditeurConditions());
        boutonReset.addActionListener(e -> resetToDefault());
        boutonExporter.addActionListener(e -> exporterResultats());
    }

    private void mettreAJourInterfaceSelonTypeCL() {
        ConditionsLimites.TypeCondition type = (ConditionsLimites.TypeCondition) comboTypeConditions.getSelectedItem();
        boolean estConstante = (type == ConditionsLimites.TypeCondition.CONSTANTES);
        spinnerValInferieure.setEnabled(estConstante);
        spinnerValSuperieure.setEnabled(estConstante);
        spinnerValGauche.setEnabled(estConstante);
        spinnerValDroite.setEnabled(estConstante);
        mettreAJourConditionsLimites(); // Mettre à jour les CL internes
    }


    /**
     * Met à jour les conditions aux limites selon l'interface (si non personnalisées)
     */
    private void mettreAJourConditionsLimites() {
        if (checkConditionsPersonnalisees.isSelected() && conditionsActuelles.getType() == ConditionsLimites.TypeCondition.PERSONNALISEES) {
            // Si "personnalisé" est coché ET que les conditions actuelles sont déjà de type PERSONNALISEES
            // (par ex. via l'éditeur avancé), on ne les écrase pas.
            // Si on a coché "personnalisé" mais que les CL actuelles sont standards, on pourrait vouloir les "figer".
            // Pour l'instant, on considère que si "perso" est coché, on n'utilise pas les spinners/combo.
            return;
        }

        ConditionsLimites.TypeCondition type = (ConditionsLimites.TypeCondition) comboTypeConditions.getSelectedItem();
        switch (type) {
            case HOMOGENES: conditionsActuelles = new ConditionsLimites(); break;
            case CONSTANTES:
                conditionsActuelles = new ConditionsLimites(
                    (Double)spinnerValInferieure.getValue(), (Double)spinnerValSuperieure.getValue(),
                    (Double)spinnerValGauche.getValue(), (Double)spinnerValDroite.getValue());
                break;
            case LINEAIRES: conditionsActuelles = ConditionsLimites.creerConditionsTest("lineaires"); break;
            case SINUSOIDALES: conditionsActuelles = ConditionsLimites.creerConditionsTest("sinusoidales"); break;
            case POLYNOMIALES: conditionsActuelles = ConditionsLimites.creerConditionsTest("polynomiales"); break;
            default: conditionsActuelles = new ConditionsLimites(); // Homogènes par défaut
        }

        if (!conditionsActuelles.verifierCompatibilite()) {
            ajouterTexteResultats("Attention: Conditions aux limites (standard) incompatibles aux coins!\n");
        }
        if (maillageActuel != null) {
            maillageActuel.setConditionsLimites(conditionsActuelles);
            panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
            panelContours.mettreAJour(maillageActuel, derniereAnalyse);
        }
        labelStatut.setText("Conditions: " + conditionsActuelles.getDescription());
    }

    /**
     * Active/désactive les contrôles standards de conditions aux limites
     */
    private void activerControlesStandardsCL(boolean activer) {
        comboTypeConditions.setEnabled(activer);
        // Les spinners sont activés/désactivés par mettreAJourInterfaceSelonTypeCL
        // en fonction de si comboTypeConditions est sur CONSTANTES ou non.
        // Ici, on s'assure juste que si activer=false, tout est désactivé.
        if (!activer) {
            spinnerValInferieure.setEnabled(false);
            spinnerValSuperieure.setEnabled(false);
            spinnerValGauche.setEnabled(false);
            spinnerValDroite.setEnabled(false);
        } else {
            // Si on active les contrôles standards, on met à jour l'état des spinners
            // en fonction du type de CL sélectionné.
            mettreAJourInterfaceSelonTypeCL();
        }
    }


    /**
     * Ouvre l'éditeur avancé de conditions aux limites
     */
    private void ouvrirEditeurConditions() {
        JDialog dialogue = new JDialog(this, "Éditeur de Conditions aux Limites Personnalisées", true);
        dialogue.setSize(600, 400);
        dialogue.setLocationRelativeTo(this);

        JPanel panelPrincipal = new JPanel(new BorderLayout(10,10));
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JTextArea descriptionArea = new JTextArea(conditionsActuelles.getDescription(), 3, 30);
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBackground(dialogue.getBackground());
        JScrollPane scrollDesc = new JScrollPane(descriptionArea);
        scrollDesc.setBorder(BorderFactory.createTitledBorder("Description Actuelle"));

        JPanel panelBoutons = new JPanel(new GridLayout(0, 2, 10, 10)); // 0 lignes = flexible
        panelBoutons.setBorder(BorderFactory.createTitledBorder("Appliquer un type prédéfini comme base"));

        String[] typesPredefinis = {"homogenes", "constantes_unitaires", "constantes_variables",
            "lineaires", "sinusoidales", "polynomiales"};
        for (String type : typesPredefinis) {
            JButton bouton = new JButton(type.replace("_", " "));
            bouton.addActionListener(e -> {
                // On crée des CL de ce type, elles deviennent les CL "personnalisées"
                conditionsActuelles = ConditionsLimites.creerConditionsTest(type);
                // On met à jour leur type interne pour refléter qu'elles sont maintenant "personnalisées"
                // Ce n'est pas idéal, car elles ne sont pas vraiment personnalisées au sens de fonctions uniques.
                // Une meilleure approche serait d'avoir une instance séparée pour les CL personnalisées par l'éditeur.
                // Pour l'instant, on se contente de mettre à jour la description.
                descriptionArea.setText("Base: " + conditionsActuelles.getDescription() + " (maintenant traitée comme personnalisée)");
            });
            panelBoutons.add(bouton);
        }

        // TODO: Ajouter des champs pour définir des FonctionsBord personnalisées (complexe)

        JPanel panelValidation = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton boutonOK = new JButton("Appliquer & Fermer");
        JButton boutonAnnuler = new JButton("Annuler");

        boutonOK.addActionListener(e -> {
            checkConditionsPersonnalisees.setSelected(true); // Coche la case "Utiliser CL personnalisées"
            activerControlesStandardsCL(false); // Désactive les contrôles standards
            // Les 'conditionsActuelles' ont été modifiées par les boutons prédéfinis ci-dessus
            if (maillageActuel != null) {
                maillageActuel.setConditionsLimites(conditionsActuelles);
                panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
                panelContours.mettreAJour(maillageActuel, derniereAnalyse);
            }
            labelStatut.setText("CL Perso: " + conditionsActuelles.getDescription());
            dialogue.dispose();
        });
        boutonAnnuler.addActionListener(e -> dialogue.dispose());
        panelValidation.add(boutonAnnuler);
        panelValidation.add(boutonOK);

        panelPrincipal.add(scrollDesc, BorderLayout.NORTH);
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
        int N_total = (Integer) spinnerTailleMaillage.getValue();

        // Mettre à jour les conditions avant de créer/configurer le maillage
        // Si "personnalisé" n'est pas coché, `mettreAJourConditionsLimites` va prendre les valeurs des spinners/combo.
        // Si "personnalisé" est coché, `conditionsActuelles` devrait déjà contenir les CL personnalisées.
        if (!checkConditionsPersonnalisees.isSelected()) {
            mettreAJourConditionsLimites();
        }
        // À ce point, `conditionsActuelles` est à jour.

        if (maillageActuel == null || maillageActuel.getN_total() != N_total) {
            maillageActuel = new Maillage(N_total, conditionsActuelles); // Constructeur applique déjà les CL
        } else {
            // Si N_total n'a pas changé, on s'assure juste que les CL sont les bonnes
            maillageActuel.setConditionsLimites(conditionsActuelles);
        }

        // configurerCasTest va réinitialiser F, exactSol, et U intérieur, puis réappliquer les CL.
        maillageActuel.configurerCasTest(casTest, conditionsActuelles);

        panelColoration.mettreAJour(maillageActuel, null);
        panelContours.mettreAJour(maillageActuel, null);
        derniereAnalyse = null; // Nouvelle config, l'ancienne analyse n'est plus valide

        ajouterTexteResultats("Configuration: " + casTest.getDescription() +
            " sur maillage " + N_total + "×" + N_total + " (N_total)\n");
        ajouterTexteResultats("Conditions aux limites: " + conditionsActuelles.getDescription() + "\n");

        if (comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) {
            double omegaOptimal = SolveurGaussSeidel.calculerOmegaOptimal(N_total);
            spinnerOmega.setValue(omegaOptimal);
        }
        labelStatut.setText("Cas configuré: " + casTest.name());
    }

    /**
     * Lance la résolution du problème avec les conditions aux limites actuelles
     */
    private void resoudreProbleme() {
        if (maillageActuel == null) {
            configurerCasTest(); // S'assure que le maillage est initialisé
        }
        // S'assurer que les CL du maillage sont bien celles de l'interface
        // (au cas où on aurait changé les CL sans re-configurer le cas)
        if (!checkConditionsPersonnalisees.isSelected()) {
            mettreAJourConditionsLimites(); // Met à jour conditionsActuelles
        }
        maillageActuel.setConditionsLimites(conditionsActuelles); // Applique au maillage

        if (!maillageActuel.verifierCoherence()) {
            JOptionPane.showMessageDialog(this,
                "Attention: Le maillage présente des incohérences (CL)!\nVérifiez les conditions aux limites.",
                "Avertissement Cohérence", JOptionPane.WARNING_MESSAGE);
        }

        setControlsEnabled(false);
        barreProgres.setValue(0);
        barreProgres.setIndeterminate(true); // Pour les solveurs sans feedback fin
        barreProgres.setVisible(true);
        labelStatut.setText("Résolution en cours...");

        SolveurGaussSeidel.MethodeResolution methode = (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();
        double tolerance = (Double) spinnerTolerance.getValue();
        int maxIter = (Integer) spinnerMaxIterations.getValue();
        double omega = (Double) spinnerOmega.getValue();

        SwingWorker<SolveurGaussSeidel.ResultatConvergence, String> worker =
            new SwingWorker<SolveurGaussSeidel.ResultatConvergence, String>() {
                @Override
                protected SolveurGaussSeidel.ResultatConvergence doInBackground() throws Exception {
                    publish("Début résolution: " + methode.getNom());
                    // Activer l'indeterminate pour les phases sans retour précis d'itération
                    // Pour un callback par itération, il faudrait adapter le solveur.
                    // Ici, on utilise un callback simple toutes les N itérations.
                    return solveur.resoudre(maillageActuel, methode, tolerance, maxIter, omega,
                        iteration -> {
                            // Ce callback est appelé par le solveur
                            // Pourrait être utilisé pour mettre à jour la barre de progression si maxIter est fixe
                            // SwingUtilities.invokeLater(() -> barreProgres.setValue(iteration * 100 / maxIter));
                            if (iteration % 50 == 0) publish("Itération " + iteration + "...");
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
                            "Erreur lors de la résolution: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                        ajouterTexteResultats("ERREUR de résolution: " + e.getMessage() + "\n");
                    } finally {
                        setControlsEnabled(true);
                        barreProgres.setIndeterminate(false);
                        barreProgres.setValue(100); // Fin
                        // labelStatut.setText("Résolution terminée."); // Sera écrasé par traiterResultatResolution
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
        sb.append("\n=== RÉSULTATS DE RÉSOLUTION ===\n");
        sb.append("Maillage N_total: ").append(maillageActuel.getN_total()).append("x").append(maillageActuel.getN_total()).append("\n");
        sb.append("Méthode: ").append(resultat.methodeUtilisee).append("\n");
        sb.append("Conditions aux limites: ").append(conditionsActuelles.getDescription()).append("\n");
        sb.append("Convergé: ").append(resultat.converge ? "OUI" : "NON").append("\n");
        sb.append("Itérations: ").append(resultat.iterations).append("\n");
        sb.append("Erreur finale (itérative): ").append(String.format("%.3e", resultat.erreurFinale)).append("\n");
        sb.append("Temps de calcul: ").append(resultat.tempsCalcul).append(" ms\n");

        double residu = solveur.calculerResidu(maillageActuel); // Recalculer sur la solution finale
        sb.append("Résidu RMS de l'équation (points intérieurs): ").append(String.format("%.3e", residu)).append("\n");

        derniereAnalyse = AnalyseurErreurs.calculerErreurs(maillageActuel);
        if (derniereAnalyse.solutionExacteDisponible) {
            sb.append("\n--- ANALYSE DES ERREURS (vs Solution Exacte) ---\n");
            sb.append("Erreur L² (intégration numérique): ").append(String.format("%.6e", derniereAnalyse.erreurL2)).append("\n");
            sb.append("Erreur maximum (points intérieurs): ").append(String.format("%.6e", derniereAnalyse.erreurMax)).append("\n");
            sb.append("Erreur moyenne (points intérieurs): ").append(String.format("%.6e", derniereAnalyse.erreurMoyenne)).append("\n");
        } else {
            sb.append("\nSolution exacte non disponible/valide pour ce cas/CL pour analyse d'erreur.\n");
        }

        double facteurConvergence = AnalyseurErreurs.analyserConvergenceIterative(resultat.historiqueErreurs);
        if (!Double.isNaN(facteurConvergence)) {
            sb.append("Facteur de convergence itérative (ρ): ").append(String.format("%.4f", facteurConvergence)).append("\n");
        }
        sb.append("--------------------------------\n");
        ajouterTexteResultats(sb.toString());

        panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
        panelContours.mettreAJour(maillageActuel, derniereAnalyse);

        labelStatut.setText("Résolution terminée. " + (resultat.converge ? "Convergé." : "Non convergé."));
        AnalyseurErreurs.analyseRapide(maillageActuel, resultat); // Affiche dans la console System.out
    }


    /**
     * Lance une analyse de convergence avec les conditions actuelles
     */
    private void analyserConvergence() {
        if (maillageActuel == null) configurerCasTest(); // S'assurer que conditionsActuelles est à jour

        Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
        SolveurGaussSeidel.MethodeResolution methode = (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();
        // Utiliser les conditions actuelles de l'interface
        final ConditionsLimites conditionsPourAnalyse = new ConditionsLimites(
            conditionsActuelles.getType() == ConditionsLimites.TypeCondition.PERSONNALISEES ?
                null : conditionsActuelles.getType().toString(), // Pour recréer, ou null pour spécifique
            0,0,0,0, conditionsActuelles.getDescription() // Un peu un hack, il faudrait mieux cloner
        );
        // Mieux: cloner conditionsActuelles si c'est une instance complexe, ou recréer si simple
        // Pour l'instant on passe conditionsActuelles directement

        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Analyse de convergence en cours...");

        SwingWorker<AnalyseurErreurs.EtudeConvergence, String> worker =
            new SwingWorker<AnalyseurErreurs.EtudeConvergence, String>() {
                @Override
                protected AnalyseurErreurs.EtudeConvergence doInBackground() throws Exception {
                    publish("Début analyse de convergence...");
                    int[] taillesN_total = {3}; // N_total (bords inclus)
                    // Passer une copie des conditions actuelles pour éviter modifs concurrentes
                    return AnalyseurErreurs.etudierConvergence(casTest, taillesN_total, methode, conditionsActuelles);
                }
                @Override
                protected void process(java.util.List<String> chunks) {
                    for(String msg : chunks) labelStatut.setText(msg);
                }
                @Override
                protected void done() {
                    try {
                        AnalyseurErreurs.EtudeConvergence etude = get();
                        afficherResultatsConvergence(etude);
                        creerGraphiqueConvergence(etude); // Afficher graphique
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(DifferencesFinis2DInterface.this,
                            "Erreur lors de l'analyse de convergence: " + e.getMessage(), "Erreur", JOptionPane.ERROR_MESSAGE);
                        ajouterTexteResultats("ERREUR Analyse Convergence: " + e.getMessage() + "\n");
                    } finally {
                        setControlsEnabled(true);
                        barreProgres.setVisible(false);
                        labelStatut.setText("Analyse de convergence terminée.");
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
        sb.append("\n=== ÉTUDE DE CONVERGENCE (CL Fixées) ===\n");
        sb.append("Cas: ").append(comboCasTest.getSelectedItem()).append("\n");
        sb.append("Méthode: ").append(comboMethode.getSelectedItem()).append("\n");
        sb.append("Conditions aux limites (appliquées à tous les maillages): \n  ").append(etude.conditionsLimites[0]).append("\n\n"); // Toutes les CL sont les mêmes

        sb.append(String.format("Ordre de convergence numérique (moyenne des ordres locaux):\n"));
        sb.append(String.format("  Norme L²: %.2f\n", etude.ordreConvergenceL2));
        sb.append(String.format("  Norme max: %.2f\n", etude.ordreConvergenceMax));
        sb.append(String.format("  (Théorique attendu pour différences finies O(h²): 2.00)\n\n"));

        sb.append("Détail par maillage (N_total):\n");
        sb.append(String.format("%-10s %-10s %-12s %-12s %-12s %-10s %-10s\n",
            "N_total", "h", "Erreur L²", "Erreur max", "E_disc_th", "Ordre L²", "Ordre Max"));
        sb.append(String.join("", Collections.nCopies(90, "-")) + "\n");

        for (int i = 0; i < etude.taillesN_total.length; i++) {
            sb.append(String.format("%-10d %-10.4f %-12.3e %-12.3e %-12.3e",
                etude.taillesN_total[i], etude.pasMaillageH[i],
                etude.erreursL2[i], etude.erreursMax[i], etude.erreursDiscretes[i]));
            if (i < etude.ordresLocauxL2.length) { // Ordres locaux pour N_i vs N_{i+1}
                sb.append(String.format(" %-10.2f %-10.2f", etude.ordresLocauxL2[i], etude.ordresLocauxMax[i]));
            } else {
                sb.append(String.format(" %-10s %-10s", "N/A", "N/A"));
            }
            sb.append("\n");
        }
        sb.append("--------------------------------\n");
        ajouterTexteResultats(sb.toString());
    }


    /**
     * Analyse l'influence des conditions aux limites
     */
    private void analyserInfluenceConditions() {
        if (maillageActuel == null) configurerCasTest(); // Pour avoir N_total

        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Analyse de l'influence des CL...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                Maillage.CasTest casTest = (Maillage.CasTest) comboCasTest.getSelectedItem();
                int N_total_courant = maillageActuel.getN_total(); // Utiliser N_total actuel
                SolveurGaussSeidel.MethodeResolution methode = (SolveurGaussSeidel.MethodeResolution) comboMethode.getSelectedItem();
                publish("=== ANALYSE DE L'INFLUENCE DES CONDITIONS AUX LIMITES ===\n");
                // La méthode dans AnalyseurErreurs va afficher ses propres résultats via System.out
                // et potentiellement ouvrir des fenêtres. On pourrait la modifier pour qu'elle "publish"
                // des strings pour la zoneResultats.
                AnalyseurErreurs.analyserInfluenceConditionsLimites(casTest, N_total_courant, methode);
                publish("\n(Détails de l'analyse d'influence des CL dans la console System.out)\n");
                return null;
            }
            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) ajouterTexteResultats(message);
            }
            @Override
            protected void done() {
                setControlsEnabled(true);
                barreProgres.setVisible(false);
                labelStatut.setText("Analyse influence CL terminée.");
            }
        };
        worker.execute();
    }


    /**
     * Compare les performances des différentes méthodes avec conditions actuelles
     */
    private void comparerMethodes() {
        if (maillageActuel == null) configurerCasTest();
        // S'assurer que les CL sont à jour
        if (!checkConditionsPersonnalisees.isSelected()) mettreAJourConditionsLimites();
        maillageActuel.setConditionsLimites(conditionsActuelles);


        setControlsEnabled(false);
        barreProgres.setVisible(true);
        barreProgres.setIndeterminate(true);
        labelStatut.setText("Comparaison des méthodes...");

        SwingWorker<Void, String> worker = new SwingWorker<Void, String>() {
            @Override
            protected Void doInBackground() throws Exception {
                StringBuilder header = new StringBuilder("\n=== COMPARAISON DES MÉTHODES (CL Fixées) ===\n");
                header.append("Conditions aux limites: ").append(conditionsActuelles.getDescription()).append("\n");
                header.append("Maillage N_total: ").append(maillageActuel.getN_total()).append("×").append(maillageActuel.getN_total()).append("\n\n");

                header.append(String.format("%-30s | %-10s | %-12s | %-10s | %-12s | %-10s | %-15s\n",
                    "Méthode", "Temps (ms)", "Itérations", "Convergé", "Err. Finale", "Résidu RMS", "Fact. Conv. (ρ)"));
                header.append(String.join("", Collections.nCopies(120, "-")) + "\n");
                publish(header.toString());

                SolveurGaussSeidel.MethodeResolution[] methodesAComparer = {
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION,
                    SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_PARALLELE
                };

                double[][] solutionInitiale = maillageActuel.copierSolution(); // Sauvegarder U initial (surtout les CL)

                for (SolveurGaussSeidel.MethodeResolution meth : methodesAComparer) {
                    maillageActuel.restaurerSolution(solutionInitiale); // Réinitialiser U intérieur et réappliquer CL

                    double tolerance = (Double) spinnerTolerance.getValue();
                    int maxIter = (Integer) spinnerMaxIterations.getValue();
                    double omegaParam = (meth == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) ?
                        (Double) spinnerOmega.getValue() : 1.0; // Utiliser la valeur du spinner pour SOR

                    SolveurGaussSeidel.ResultatConvergence res =
                        solveur.resoudre(maillageActuel, meth, tolerance, maxIter, omegaParam,
                            iter -> { if (iter % 200 == 0) publishState("Test " + meth.getNom() + " iter " + iter); });

                    double residuFinal = solveur.calculerResidu(maillageActuel); // Sur la solution finale
                    double facteurConv = AnalyseurErreurs.analyserConvergenceIterative(res.historiqueErreurs);

                    AnalyseurErreurs.ResultatAnalyse analyseErr = AnalyseurErreurs.calculerErreurs(maillageActuel);
                    String errL2Str = analyseErr.solutionExacteDisponible ? String.format("%.3e", analyseErr.erreurL2) : "N/A";

                    String row = String.format("%-30s | %-10d | %-12d | %-10s | %-12.3e | %-10.3e | %-15s\n",
                        res.methodeUtilisee, // Utilise le nom complet avec omega si SOR
                        res.tempsCalcul,
                        res.iterations,
                        (res.converge ? "OUI" : "NON"),
                        res.erreurFinale,
                        residuFinal,
                        (Double.isNaN(facteurConv) ? "N/A" : String.format("%.4f", facteurConv))
                        // Potentiellement ajouter: + " (ErrL2: " + errL2Str + ")"
                    );
                    publish(row);
                }
                publish("--------------------------------\n");
                maillageActuel.restaurerSolution(solutionInitiale); // Restaurer une dernière fois
                return null;
            }
            // Méthode pour mettre à jour le labelStatut pendant le doInBackground
            private void publishState(String state) { SwingUtilities.invokeLater(() -> labelStatut.setText(state)); }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String message : chunks) ajouterTexteResultats(message);
            }
            @Override
            protected void done() {
                setControlsEnabled(true);
                barreProgres.setVisible(false);
                labelStatut.setText("Comparaison des méthodes terminée.");
                // Mettre à jour les visualisations avec la dernière solution (celle du dernier test de méthode)
                panelColoration.mettreAJour(maillageActuel, derniereAnalyse);
                panelContours.mettreAJour(maillageActuel, derniereAnalyse);
            }
        };
        worker.execute();
    }


    /**
     * Remet l'interface à son état par défaut
     */
    private void resetToDefault() {
        comboCasTest.setSelectedItem(Maillage.CasTest.CAS1);
        spinnerTailleMaillage.setValue(20); // N_total
        comboMethode.setSelectedItem(SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_CLASSIQUE);
        spinnerTolerance.setValue(1e-6);
        spinnerMaxIterations.setValue(5000);
        spinnerOmega.setValue(1.5); // Valeur typique pour SOR

        comboTypeConditions.setSelectedItem(ConditionsLimites.TypeCondition.HOMOGENES);
        spinnerValInferieure.setValue(0.0);
        spinnerValSuperieure.setValue(0.0);
        spinnerValGauche.setValue(0.0);
        spinnerValDroite.setValue(0.0);
        checkConditionsPersonnalisees.setSelected(false);
        activerControlesStandardsCL(true); // Active les contrôles standards
        // et met à jour les CL internes via mettreAJourInterfaceSelonTypeCL

        // `conditionsActuelles` est mis à jour par `mettreAJourInterfaceSelonTypeCL` appelé par `activerControlesStandardsCL`

        zoneResultats.setText("");
        derniereAnalyse = null;

        configurerCasTest(); // Configure le maillage et met à jour l'UI

        labelStatut.setText("Interface réinitialisée. Conditions par défaut.");
        ajouterTexteResultats("=== SOLVEUR DIFFÉRENCES FINIES 2D (N est N_total) ===\n");
        ajouterTexteResultats("Équation résolue: -ΔU = f sur [0,1]x[0,1]\n");
        ajouterTexteResultats("Conditions aux limites: Dirichlet générales U(bord) = valeurs spécifiées\n");
        ajouterTexteResultats("Discrétisation: Schéma 5 points\n\n");
    }


    /**
     * Exporte les résultats vers un fichier avec informations sur les conditions
     */
    private void exporterResultats() {
        if (maillageActuel == null) {
            JOptionPane.showMessageDialog(this, "Aucun maillage à exporter. Veuillez d'abord résoudre un problème.",
                "Exportation impossible", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Exporter le rapport et la solution");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Fichiers texte (*.txt)", "txt"));
        chooser.setSelectedFile(new java.io.File("Rapport_DF2D_" + comboCasTest.getSelectedItem() + "_N" + maillageActuel.getN_total() + ".txt"));


        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".txt")) {
                    file = new java.io.File(file.getAbsolutePath() + ".txt");
                }

                try (java.io.PrintWriter writer = new java.io.PrintWriter(new java.io.FileWriter(file))) {
                    writer.println("=== RAPPORT DIFFÉRENCES FINIES 2D (N est N_total) ===");
                    writer.println("Date: " + new java.util.Date());
                    writer.println();

                    writer.println("CONFIGURATION DU PROBLÈME:");
                    writer.println("  Cas de test: " + comboCasTest.getSelectedItem());
                    writer.println("  Taille maillage N_total: " + maillageActuel.getN_total() + "×" + maillageActuel.getN_total());
                    writer.println("  Pas h: " + String.format("%.6f", maillageActuel.getH()));
                    writer.println("  Nombre de points intérieurs N_interieur: " + maillageActuel.getN_interieur() + "×" + maillageActuel.getN_interieur());
                    writer.println();

                    writer.println("PARAMÈTRES DE RÉSOLUTION:");
                    writer.println("  Méthode: " + comboMethode.getSelectedItem());
                    writer.println("  Tolérance: " + spinnerTolerance.getValue());
                    writer.println("  Max Itérations: " + spinnerMaxIterations.getValue());
                    if (comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION) {
                        writer.println("  Paramètre Omega (SOR): " + spinnerOmega.getValue());
                    }
                    writer.println();

                    writer.println("CONDITIONS AUX LIMITES:");
                    writer.println("  Type: " + conditionsActuelles.getDescription());
                    writer.println("  Compatibilité aux coins: " + (conditionsActuelles.verifierCompatibilite() ? "OK" : "Problème"));
                    if (conditionsActuelles.getType() == ConditionsLimites.TypeCondition.CONSTANTES) {
                        writer.println("  Valeurs (Inf, Sup, Gauche, Droite): " +
                            String.format("%.2f, %.2f, %.2f, %.2f", conditionsActuelles.getValeurInferieure(),
                                conditionsActuelles.getValeurSuperieure(), conditionsActuelles.getValeurGauche(),
                                conditionsActuelles.getValeurDroite()));
                    }
                    writer.println();

                    writer.println("RÉSULTATS DÉTAILLÉS (CONSOLE DE L'INTERFACE):");
                    writer.println(zoneResultats.getText().replace("\n", System.lineSeparator())); // Normaliser les fins de ligne
                    writer.println();

                    if (derniereAnalyse != null) {
                        writer.println(AnalyseurErreurs.genererRapportComplet(derniereAnalyse, null, maillageActuel)
                            .replace("\n", System.lineSeparator()));
                    } else {
                        writer.println("Aucune analyse d'erreur détaillée disponible (solution exacte non fournie ou non calculée).");
                    }
                    writer.println();

                    writer.println("SOLUTION NUMÉRIQUE U[i][j] (indices globaux, i=ligne, j=colonne):");
                    double[][] U = maillageActuel.getU();
                    int N_total_export = maillageActuel.getN_total();
                    int step = Math.max(1, N_total_export / 10); // Afficher environ 10x10 points
                    for (int i = 0; i < N_total_export; i += step) {
                        for (int j = 0; j < N_total_export; j += step) {
                            writer.printf("U[%3d][%3d] = % .6e   ", i, j, U[i][j]);
                        }
                        writer.println();
                    }
                    if (N_total_export-1 % step !=0) { // S'assurer que le dernier point est affiché
                        writer.println("...");
                        for (int j = 0; j < N_total_export; j += step) {
                            writer.printf("U[%3d][%3d] = % .6e   ", N_total_export-1, j, U[N_total_export-1][j]);
                        }
                        writer.println();
                        for (int i = 0; i < N_total_export-1; i += step) { // -1 car dernier point de la ligne déjà fait
                            writer.printf("U[%3d][%3d] = % .6e   ", i, N_total_export-1, U[i][N_total_export-1]);
                        }
                        writer.printf("U[%3d][%3d] = % .6e\n", N_total_export-1, N_total_export-1, U[N_total_export-1][N_total_export-1]);
                    }
                }
                JOptionPane.showMessageDialog(this, "Rapport exporté vers: " + file.getAbsolutePath(),
                    "Exportation Réussie", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException ex) {
                JOptionPane.showMessageDialog(this, "Erreur lors de l'exportation: " + ex.getMessage(),
                    "Erreur Exportation", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }


    /**
     * Crée un graphique de convergence
     */
    private void creerGraphiqueConvergence(AnalyseurErreurs.EtudeConvergence etude) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetreGraphique = new JFrame("Étude de Convergence - " + conditionsActuelles.getDescription());
            fenetreGraphique.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            fenetreGraphique.setSize(800, 600); // Plus grand
            fenetreGraphique.setLocationRelativeTo(this);

            JPanel panelGraphique = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    dessinerGraphiqueConvergence((Graphics2D)g, etude); // Cast en Graphics2D
                }
            };
            panelGraphique.setBackground(Color.WHITE);

            JLabel labelInfo = new JLabel("<html><b>Cas:</b> " + comboCasTest.getSelectedItem() +
                "<br><b>Conditions aux limites:</b> " + etude.conditionsLimites[0] + "</html>");
            labelInfo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            fenetreGraphique.setLayout(new BorderLayout());
            fenetreGraphique.add(labelInfo, BorderLayout.NORTH);
            fenetreGraphique.add(panelGraphique, BorderLayout.CENTER);
            fenetreGraphique.setVisible(true);
        });
    }

    /**
     * Dessine le graphique de convergence avec informations sur les conditions
     */
    private void dessinerGraphiqueConvergence(Graphics2D g2d, AnalyseurErreurs.EtudeConvergence etude) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int padding = 60; int labelPadding = 25;
        int width = getWidth() - 2 * padding - labelPadding;
        int height = getHeight() - 2 * padding - labelPadding;

        // Titre du graphique
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        String titreGraph = "Convergence: Erreur vs h (log-log)";
        FontMetrics fmTitre = g2d.getFontMetrics();
        g2d.drawString(titreGraph, (getWidth() - fmTitre.stringWidth(titreGraph))/2, padding - labelPadding/2);


        // Trouver min/max pour les échelles log
        double minH = Arrays.stream(etude.pasMaillageH).filter(h -> h > 0).min().orElse(0.01);
        double maxH = Arrays.stream(etude.pasMaillageH).max().orElse(1.0);
        double minErr = Arrays.stream(etude.erreursL2).filter(e -> e > 1e-15 && !Double.isNaN(e)).min().orElse(1e-10);
        minErr = Math.min(minErr, Arrays.stream(etude.erreursMax).filter(e -> e > 1e-15 && !Double.isNaN(e)).min().orElse(1e-10));
        double maxErr = Arrays.stream(etude.erreursL2).filter(e -> !Double.isNaN(e)).max().orElse(1.0);
        maxErr = Math.max(maxErr, Arrays.stream(etude.erreursMax).filter(e -> !Double.isNaN(e)).max().orElse(1.0));

        if (minErr == 0) minErr = 1e-10; // Eviter log(0)
        if (maxErr == 0) maxErr = 1.0;
        if (minH == 0) minH = 0.01;

        // Axes
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawLine(padding, padding + height, padding + width, padding + height); // Axe X (h)
        g2d.drawLine(padding, padding, padding, padding + height);                   // Axe Y (Erreur)

        // Labels des axes
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("log(h)", padding + width / 2, padding + height + labelPadding);
        AffineTransform oldTransform = g2d.getTransform();
        g2d.rotate(-Math.PI / 2);
        g2d.drawString("log(Erreur)", -(padding + height / 2), padding - labelPadding / 2 - 5);
        g2d.setTransform(oldTransform);

        // Fonction pour convertir les coordonnées log-log en écran
        double finalMinH = minH;
        ScaleConverter scaleX = (val) -> padding + (Math.log(val) - Math.log(finalMinH)) / (Math.log(maxH) - Math.log(finalMinH)) * width;
        double finalMinErr = minErr;
        double finalMaxErr = maxErr;
        ScaleConverter scaleY = (val) -> padding + height - (Math.log(val) - Math.log(finalMinErr)) / (Math.log(finalMaxErr) - Math.log(finalMinErr)) * height;

        // Dessiner les courbes
        plotCurve(g2d, etude.pasMaillageH, etude.erreursL2, Color.BLUE, scaleX, scaleY);
        plotCurve(g2d, etude.pasMaillageH, etude.erreursMax, Color.RED, scaleX, scaleY);
        plotCurve(g2d, etude.pasMaillageH, etude.erreursDiscretes, Color.GREEN, scaleX, scaleY, new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{5}, 0));

        // Légende
        int legendX = padding + width - 200;
        int legendY = padding + 20;
        g2d.setColor(Color.BLUE); g2d.drawString("Erreur L² (Ordre: " + String.format("%.2f", etude.ordreConvergenceL2) + ")", legendX, legendY);
        g2d.setColor(Color.RED); g2d.drawString("Erreur max (Ordre: " + String.format("%.2f", etude.ordreConvergenceMax) + ")", legendX, legendY + 15);
        g2d.setColor(Color.GREEN); g2d.drawString("Erreur discrétisation (théorique O(h²))", legendX, legendY + 30);
    }

    @FunctionalInterface
    interface ScaleConverter { double convert(double value); }

    private void plotCurve(Graphics2D g2d, double[] xData, double[] yData, Color color, ScaleConverter scaleX, ScaleConverter scaleY) {
        plotCurve(g2d, xData, yData, color, scaleX, scaleY, new BasicStroke(2f));
    }
    private void plotCurve(Graphics2D g2d, double[] xData, double[] yData, Color color, ScaleConverter scaleX, ScaleConverter scaleY, Stroke stroke) {
        g2d.setColor(color);
        g2d.setStroke(stroke);
        Path2D path = new Path2D.Double();
        boolean firstPoint = true;
        for (int i = 0; i < xData.length; i++) {
            if (xData[i] > 0 && yData[i] > 1e-15 && !Double.isNaN(yData[i])) { // Valide pour log
                double x1 = scaleX.convert(xData[i]);
                double y1 = scaleY.convert(yData[i]);
                if (firstPoint) {
                    path.moveTo(x1, y1);
                    firstPoint = false;
                } else {
                    path.lineTo(x1, y1);
                }
                g2d.fillOval((int)x1 - 3, (int)y1 - 3, 6, 6); // Marquer les points
            }
        }
        g2d.draw(path);
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
        spinnerOmega.setEnabled(enabled && comboMethode.getSelectedItem() == SolveurGaussSeidel.MethodeResolution.GAUSS_SEIDEL_RELAXATION);

        activerControlesStandardsCL(enabled && !checkConditionsPersonnalisees.isSelected());
        boutonConditionsAvancees.setEnabled(enabled);
        checkConditionsPersonnalisees.setEnabled(enabled);

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
        SwingUtilities.invokeLater(() -> { // S'assurer que c'est fait sur l'EDT
            zoneResultats.append(texte);
            zoneResultats.setCaretPosition(zoneResultats.getDocument().getLength());
        });
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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new DifferencesFinis2DInterface().setVisible(true));
    }
}
