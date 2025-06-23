import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour la visualisation graphique des solutions
 *
 * Implémente deux types de représentation :
 * 1. Système de coloration (heatmap) Représentation par couleurs
 * 2. Courbes de niveau (contours) Isolignes de la solution
 *
 * La visualisation permet d'analyser visuellement :
 *     La distribution de la solution dans le domaine
 *     Les gradients et variations locales
 *     La régularité de la solution
 *     La présence d'oscillations numériques
 */
public class VisualiseurGraphique {

    /**
     * Énumération des types de visualisation disponibles
     */
    public enum TypeVisualisation {
        COLORATION("Système de Coloration (Heatmap)"),
        COURBES_NIVEAU("Courbes de Niveau (Contours)"),
        SURFACE_3D("Surface 3D"),
        ERREURS("Carte des Erreurs");

        private final String nom;

        TypeVisualisation(String nom) {
            this.nom = nom;
        }

        public String getNom() { return nom; }
    }

    /**
     * Interface pour les callbacks de mise à jour
     */
    public interface CallbackMiseAJour {
        void surMiseAJour(String message);
    }

    /**
     * Panel principal de visualisation
     */
    public static class PanelVisualisation extends JPanel {
        private Maillage maillage;
        private TypeVisualisation typeVisu;
        private AnalyseurErreurs.ResultatAnalyse analyseErreurs;
        private boolean afficherGrille = true;
        private boolean afficherValeurs = false;
        private double[] niveauxContours;

        public PanelVisualisation(int largeur, int hauteur) {
            setPreferredSize(new Dimension(largeur, hauteur));
            setBackground(Color.WHITE);
            this.typeVisu = TypeVisualisation.COLORATION;
        }

        /**
         * Met à jour les données à visualiser
         */
        public void mettreAJour(Maillage maillage, AnalyseurErreurs.ResultatAnalyse analyseErreurs) {
            this.maillage = maillage;
            this.analyseErreurs = analyseErreurs;
            calculerNiveauxContours();
            repaint();
        }

        /**
         * Change le type de visualisation
         */
        public void setTypeVisualisation(TypeVisualisation type) {
            this.typeVisu = type;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (maillage == null) {
                dessinerMessageAttente(g);
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            switch (typeVisu) {
                case COLORATION:
                    dessinerColoration(g2d);
                    break;
                case COURBES_NIVEAU:
                    dessinerCourbesNiveau(g2d);
                    break;
                case ERREURS:
                    dessinerCarteErreurs(g2d);
                    break;
            }

            dessinerLegende(g2d);
            dessinerInformations(g2d);
        }

        /**
         * Dessine la représentation par système de coloration (heatmap)
         *
         * Principe : Chaque point du maillage est coloré selon sa valeur
         * - Bleu foncé : valeurs faibles
         * - Bleu clair → Cyan → Vert → Jaune → Rouge : valeurs croissantes
         * - Rouge foncé : valeurs élevées
         */
        private void dessinerColoration(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N = maillage.getN();

            // Calcul des dimensions d'affichage
            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge - 150; // 150 pour la légende
            int hauteurDispo = getHeight() - 2 * marge - 50; // 50 pour les infos

            int tailleCellule = Math.min(largeurDispo / (N + 2), hauteurDispo / (N + 2));
            int offsetX = marge;
            int offsetY = marge;

            // Détermination des valeurs min/max pour la normalisation
            double[] minMax = calculerMinMax(U);
            double minVal = minMax[0];
            double maxVal = minMax[1];

            System.out.println("Visualisation par coloration: min=" + minVal + ", max=" + maxVal);

            // Dessin du maillage coloré
            for (int i = 0; i <= N + 1; i++) {
                for (int j = 0; j <= N + 1; j++) {
                    int x = offsetX + j * tailleCellule;
                    int y = offsetY + (N + 1 - i) * tailleCellule; // Inversion Y

                    Color couleurCellule;
                    if (maillage.isBoundary(i, j)) {
                        couleurCellule = Color.BLACK; // Conditions aux limites
                    } else {
                        double valeurNormalisee = normaliser(U[i][j], minVal, maxVal);
                        couleurCellule = getCouleurHeatmap(valeurNormalisee);
                    }

                    // Remplissage de la cellule
                    g2d.setColor(couleurCellule);
                    g2d.fillRect(x, y, tailleCellule, tailleCellule);

                    // Contour de la cellule si grille activée
                    if (afficherGrille) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.drawRect(x, y, tailleCellule, tailleCellule);
                    }

                    // Affichage des valeurs si demandé et si assez de place
                    if (afficherValeurs && tailleCellule > 30) {
                        g2d.setColor(Color.WHITE);
                        g2d.setFont(new Font("Arial", Font.PLAIN, 8));
                        String valeurStr = String.format("%.2f", U[i][j]);
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = x + (tailleCellule - fm.stringWidth(valeurStr)) / 2;
                        int textY = y + (tailleCellule + fm.getAscent()) / 2;
                        g2d.drawString(valeurStr, textX, textY);
                    }
                }
            }

            // Dessin de la barre de couleur (légende)
            dessinerBarreCouleur(g2d, offsetX + (N + 2) * tailleCellule + 20, offsetY, minVal, maxVal);
        }

        /**
         * Dessine les courbes de niveau (contours)
         *
         * Principe : Trace des lignes d'égale valeur (isolignes)
         * Utilise l'algorithme marching squares pour l'interpolation
         */
        private void dessinerCourbesNiveau(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N = maillage.getN();

            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge;
            int hauteurDispo = getHeight() - 2 * marge - 50;

            double echelleX = (double) largeurDispo / (N + 1);
            double echelleY = (double) hauteurDispo / (N + 1);

            // Fond blanc
            g2d.setColor(Color.WHITE);
            g2d.fillRect(marge, marge, largeurDispo, hauteurDispo);

            // Dessin du contour du domaine
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(marge, marge, largeurDispo, hauteurDispo);

            // Calcul et dessin des courbes de niveau
            if (niveauxContours != null) {
                Color[] couleursContours = genererCouleursContours(niveauxContours.length);

                for (int k = 0; k < niveauxContours.length; k++) {
                    double niveau = niveauxContours[k];
                    g2d.setColor(couleursContours[k]);
                    g2d.setStroke(new BasicStroke(1.5f));

                    // Algorithme simplifié de contours (interpolation linéaire)
                    for (int i = 1; i <= N; i++) {
                        for (int j = 1; j <= N; j++) {
                            dessinerContourCellule(g2d, U, i, j, niveau, marge, echelleX, echelleY, N);
                        }
                    }
                }
            }

            // Grille optionnelle
            if (afficherGrille) {
                g2d.setColor(new Color(200, 200, 200, 100));
                g2d.setStroke(new BasicStroke(0.5f));

                for (int i = 0; i <= N + 1; i++) {
                    int x = (int) (marge + i * echelleX);
                    int y = (int) (marge + i * echelleY);
                    g2d.drawLine(x, marge, x, marge + hauteurDispo);
                    g2d.drawLine(marge, y, marge + largeurDispo, y);
                }
            }
        }

        /**
         * Dessine la carte des erreurs locales
         */
        private void dessinerCarteErreurs(Graphics2D g2d) {
            if (analyseErreurs == null || analyseErreurs.carteErreurs == null) {
                g2d.setColor(Color.RED);
                g2d.drawString("Aucune analyse d'erreur disponible", 50, 50);
                return;
            }

            double[][] erreurs = analyseErreurs.carteErreurs;
            int N = maillage.getN();

            int marge = 50;
            int tailleCellule = Math.min((getWidth() - 2 * marge - 150) / (N + 2),
                (getHeight() - 2 * marge - 50) / (N + 2));

            // Normalisation des erreurs pour la visualisation
            double[] minMax = calculerMinMax(erreurs);
            double maxErreur = minMax[1];

            for (int i = 1; i <= N; i++) {
                for (int j = 1; j <= N; j++) {
                    int x = marge + j * tailleCellule;
                    int y = marge + (N + 1 - i) * tailleCellule;

                    if (!maillage.isBoundary(i, j)) {
                        double erreurNormalisee = erreurs[i][j] / maxErreur;
                        Color couleur = getCouleurErreur(erreurNormalisee);

                        g2d.setColor(couleur);
                        g2d.fillRect(x, y, tailleCellule, tailleCellule);
                    } else {
                        g2d.setColor(Color.BLACK);
                        g2d.fillRect(x, y, tailleCellule, tailleCellule);
                    }

                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawRect(x, y, tailleCellule, tailleCellule);
                }
            }

            // Légende pour les erreurs
            dessinerLegendErreurs(g2d, marge + (N + 2) * tailleCellule + 20, marge, maxErreur);
        }

        /**
         * Calcule les niveaux pour les courbes de niveau
         */
        private void calculerNiveauxContours() {
            if (maillage == null) return;

            double[] minMax = calculerMinMax(maillage.getU());
            double minVal = minMax[0];
            double maxVal = minMax[1];

            int nbNiveaux = 10;
            niveauxContours = new double[nbNiveaux];

            for (int i = 0; i < nbNiveaux; i++) {
                niveauxContours[i] = minVal + (maxVal - minVal) * i / (nbNiveaux - 1);
            }
        }

        /**
         * Dessine un contour dans une cellule par interpolation linéaire
         */
        private void dessinerContourCellule(Graphics2D g2d, double[][] U, int i, int j, double niveau,
                                            int marge, double echelleX, double echelleY, int N) {

            // Valeurs aux quatre coins de la cellule
            double v00 = U[i][j];     // Coin bas-gauche
            double v10 = U[i][j+1];   // Coin bas-droit
            double v01 = U[i+1][j];   // Coin haut-gauche
            double v11 = U[i+1][j+1]; // Coin haut-droit

            // Points d'intersection potentiels avec les bords de la cellule
            List<Point2D.Double> intersections = new ArrayList<>();

            // Bord gauche (entre v00 et v01)
            if ((v00 <= niveau && niveau <= v01) || (v01 <= niveau && niveau <= v00)) {
                if (Math.abs(v01 - v00) > 1e-10) {
                    double t = (niveau - v00) / (v01 - v00);
                    double x = marge + j * echelleX;
                    double y = marge + (N + 1 - i - t) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord droit (entre v10 et v11)
            if ((v10 <= niveau && niveau <= v11) || (v11 <= niveau && niveau <= v10)) {
                if (Math.abs(v11 - v10) > 1e-10) {
                    double t = (niveau - v10) / (v11 - v10);
                    double x = marge + (j + 1) * echelleX;
                    double y = marge + (N + 1 - i - t) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord bas (entre v00 et v10)
            if ((v00 <= niveau && niveau <= v10) || (v10 <= niveau && niveau <= v00)) {
                if (Math.abs(v10 - v00) > 1e-10) {
                    double t = (niveau - v00) / (v10 - v00);
                    double x = marge + (j + t) * echelleX;
                    double y = marge + (N + 1 - i) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord haut (entre v01 et v11)
            if ((v01 <= niveau && niveau <= v11) || (v11 <= niveau && niveau <= v01)) {
                if (Math.abs(v11 - v01) > 1e-10) {
                    double t = (niveau - v01) / (v11 - v01);
                    double x = marge + (j + t) * echelleX;
                    double y = marge + (N + 1 - i - 1) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Dessiner les segments de contour
            if (intersections.size() >= 2) {
                Point2D.Double p1 = intersections.get(0);
                Point2D.Double p2 = intersections.get(1);
                g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
            }
        }

        /**
         * Génère des couleurs distinctes pour les contours
         */
        private Color[] genererCouleursContours(int nbCouleurs) {
            Color[] couleurs = new Color[nbCouleurs];
            for (int i = 0; i < nbCouleurs; i++) {
                float hue = (float) i / nbCouleurs;
                couleurs[i] = Color.getHSBColor(hue, 0.8f, 0.8f);
            }
            return couleurs;
        }

        /**
         * Dessine la barre de couleur (légende)
         */
        private void dessinerBarreCouleur(Graphics2D g2d, int x, int y, double minVal, double maxVal) {
            int largeur = 20;
            int hauteur = 200;

            // Gradient de couleurs
            for (int i = 0; i < hauteur; i++) {
                double valeurNormalisee = (double) i / hauteur;
                Color couleur = getCouleurHeatmap(valeurNormalisee);
                g2d.setColor(couleur);
                g2d.fillRect(x, y + hauteur - i, largeur, 1);
            }

            // Contour
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, largeur, hauteur);

            // Étiquettes
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(String.format("%.3f", maxVal), x + largeur + 5, y + 5);
            g2d.drawString(String.format("%.3f", (maxVal + minVal) / 2), x + largeur + 5, y + hauteur / 2);
            g2d.drawString(String.format("%.3f", minVal), x + largeur + 5, y + hauteur + 5);
        }

        /**
         * Dessine la légende pour les erreurs
         */
        private void dessinerLegendErreurs(Graphics2D g2d, int x, int y, double maxErreur) {
            int largeur = 20;
            int hauteur = 200;

            for (int i = 0; i < hauteur; i++) {
                double valeurNormalisee = (double) i / hauteur;
                Color couleur = getCouleurErreur(valeurNormalisee);
                g2d.setColor(couleur);
                g2d.fillRect(x, y + hauteur - i, largeur, 1);
            }

            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, largeur, hauteur);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(String.format("%.2e", maxErreur), x + largeur + 5, y + 5);
            g2d.drawString(String.format("%.2e", maxErreur / 2), x + largeur + 5, y + hauteur / 2);
            g2d.drawString("0.0", x + largeur + 5, y + hauteur + 5);
        }

        /**
         * Dessine les informations générales
         */
        private void dessinerInformations(Graphics2D g2d) {
            if (maillage == null) return;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            String info = String.format("Maillage: %dx%d, h=%.4f, Type: %s",
                maillage.getN() + 2, maillage.getN() + 2,
                maillage.getH(), typeVisu.getNom());
            g2d.drawString(info, 10, getHeight() - 10);
        }

        /**
         * Dessine un message d'attente
         */
        private void dessinerMessageAttente(Graphics g) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.ITALIC, 16));
            String message = "En attente de données...";
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(message)) / 2;
            int y = getHeight() / 2;
            g.drawString(message, x, y);
        }

        /**
         * Dessine une légende générale
         */
        private void dessinerLegende(Graphics2D g2d) {
            // Implémentation selon le type de visualisation
        }

        // Méthodes utilitaires

        /**
         * Calcule les valeurs min et max d'une matrice
         */
        private double[] calculerMinMax(double[][] matrice) {
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            for (int i = 0; i < matrice.length; i++) {
                for (int j = 0; j < matrice[i].length; j++) {
                    min = Math.min(min, matrice[i][j]);
                    max = Math.max(max, matrice[i][j]);
                }
            }

            return new double[]{min, max};
        }

        /**
         * Normalise une valeur entre 0 et 1
         */
        private double normaliser(double valeur, double min, double max) {
            if (Math.abs(max - min) < 1e-12) return 0.5;
            return (valeur - min) / (max - min);
        }

        /**
         * Génère une couleur pour la heatmap basée sur une valeur normalisée
         */
        private Color getCouleurHeatmap(double valeurNormalisee) {
            // Palette: Bleu → Cyan → Vert → Jaune → Rouge
            if (valeurNormalisee < 0.25) {
                float ratio = (float) (valeurNormalisee / 0.25);
                return new Color(0, (int) (ratio * 255), 255);
            } else if (valeurNormalisee < 0.5) {
                float ratio = (float) ((valeurNormalisee - 0.25) / 0.25);
                return new Color(0, 255, (int) (255 * (1 - ratio)));
            } else if (valeurNormalisee < 0.75) {
                float ratio = (float) ((valeurNormalisee - 0.5) / 0.25);
                return new Color((int) (ratio * 255), 255, 0);
            } else {
                float ratio = (float) ((valeurNormalisee - 0.75) / 0.25);
                return new Color(255, (int) (255 * (1 - ratio)), 0);
            }
        }

        /**
         * Génère une couleur pour la visualisation des erreurs
         */
        private Color getCouleurErreur(double valeurNormalisee) {
            // Palette: Blanc (pas d'erreur) → Jaune → Orange → Rouge (erreur max)
            if (valeurNormalisee < 0.33) {
                float ratio = (float) (valeurNormalisee / 0.33);
                return new Color(255, 255, (int) (255 * (1 - ratio)));
            } else if (valeurNormalisee < 0.66) {
                float ratio = (float) ((valeurNormalisee - 0.33) / 0.33);
                return new Color(255, (int) (255 * (1 - ratio * 0.5)), 0);
            } else {
                float ratio = (float) ((valeurNormalisee - 0.66) / 0.34);
                return new Color(255, (int) (128 * (1 - ratio)), 0);
            }
        }

        // Getters et setters

        public void setAfficherGrille(boolean afficher) {
            this.afficherGrille = afficher;
            repaint();
        }

        public void setAfficherValeurs(boolean afficher) {
            this.afficherValeurs = afficher;
            repaint();
        }

        public boolean isAfficherGrille() { return afficherGrille; }
        public boolean isAfficherValeurs() { return afficherValeurs; }
    }

    /**
     * Crée une fenêtre de visualisation indépendante
     */
    public static JFrame creerFenetreVisualisation(String titre, Maillage maillage,
                                                   AnalyseurErreurs.ResultatAnalyse analyse,
                                                   TypeVisualisation typeInitial) {
        JFrame fenetre = new JFrame(titre);
        fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        PanelVisualisation panel = new PanelVisualisation(800, 600);
        panel.setTypeVisualisation(typeInitial);
        panel.mettreAJour(maillage, analyse);

        // Panel de contrôles
        JPanel controles = new JPanel(new FlowLayout());

        JComboBox<TypeVisualisation> comboType = new JComboBox<>(TypeVisualisation.values());
        comboType.setSelectedItem(typeInitial);
        comboType.addActionListener(e -> {
            TypeVisualisation type = (TypeVisualisation) comboType.getSelectedItem();
            panel.setTypeVisualisation(type);
        });

        JCheckBox checkGrille = new JCheckBox("Grille", panel.isAfficherGrille());
        checkGrille.addActionListener(e -> panel.setAfficherGrille(checkGrille.isSelected()));

        JCheckBox checkValeurs = new JCheckBox("Valeurs", panel.isAfficherValeurs());
        checkValeurs.addActionListener(e -> panel.setAfficherValeurs(checkValeurs.isSelected()));

        JButton boutonExporter = new JButton("Exporter");
        boutonExporter.addActionListener(e -> exporterImage(panel));

        controles.add(new JLabel("Type:"));
        controles.add(comboType);
        controles.add(checkGrille);
        controles.add(checkValeurs);
        controles.add(boutonExporter);

        fenetre.setLayout(new BorderLayout());
        fenetre.add(controles, BorderLayout.NORTH);
        fenetre.add(panel, BorderLayout.CENTER);

        fenetre.pack();
        fenetre.setLocationRelativeTo(null);

        return fenetre;
    }

    /**
     * Exporte l'image de visualisation
     */
    private static void exporterImage(PanelVisualisation panel) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images PNG", "png"));

        if (chooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
            try {
                java.awt.image.BufferedImage image = new java.awt.image.BufferedImage(
                    panel.getWidth(), panel.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = image.createGraphics();
                panel.paint(g2d);
                g2d.dispose();

                java.io.File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new java.io.File(file.getAbsolutePath() + ".png");
                }

                javax.imageio.ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(panel, "Image exportée: " + file.getAbsolutePath());

            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(panel, "Erreur lors de l'export: " + e.getMessage());
            }
        }
    }

    /**
     * Méthode utilitaire pour créer rapidement une visualisation
     */
    public static void visualiser(Maillage maillage, AnalyseurErreurs.ResultatAnalyse analyse, String titre) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = creerFenetreVisualisation(titre, maillage, analyse, TypeVisualisation.COLORATION);
            fenetre.setVisible(true);
        });
    }

    /**
     * Crée un panel de comparaison de plusieurs visualisations
     */
    public static JPanel creerPanelComparaison(Maillage[] maillages, String[] titres,
                                               AnalyseurErreurs.ResultatAnalyse[] analyses) {
        JPanel panelPrincipal = new JPanel(new GridLayout(2, 2, 5, 5));

        for (int i = 0; i < Math.min(4, maillages.length); i++) {
            JPanel panelIndividuel = new JPanel(new BorderLayout());

            PanelVisualisation panel = new PanelVisualisation(300, 300);
            panel.mettreAJour(maillages[i], analyses != null ? analyses[i] : null);

            JLabel titre = new JLabel(titres[i], JLabel.CENTER);
            titre.setFont(new Font("Arial", Font.BOLD, 12));

            panelIndividuel.add(titre, BorderLayout.NORTH);
            panelIndividuel.add(panel, BorderLayout.CENTER);
            panelIndividuel.setBorder(BorderFactory.createEtchedBorder());

            panelPrincipal.add(panelIndividuel);
        }

        return panelPrincipal;
    }
}
