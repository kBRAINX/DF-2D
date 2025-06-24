import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour la visualisation graphique des solutions avec conditions aux limites générales
 *
 * Implémente plusieurs types de représentation :
 * 1. Système de coloration (heatmap) Représentation par couleurs
 * 2. Courbes de niveau (contours) Isolignes de la solution
 * 3. Surface 3D - Projection isométrique
 * 4. Carte des erreurs - Visualisation des erreurs locales
 *
 * Adaptations pour les conditions aux limites générales :
 * - Affichage des valeurs des conditions aux limites
 * - Mise en évidence des variations aux bords
 * - Analyse de la continuité aux interfaces
 * - Visualisation de l'influence des conditions sur la solution
 */
public class VisualiseurGraphique {

    /**
     * Énumération des types de visualisation disponibles
     */
    public enum TypeVisualisation {
        COLORATION("Système de Coloration (Heatmap)"),
        COURBES_NIVEAU("Courbes de Niveau (Contours)"),
        ERREURS("Carte des Erreurs"),
        CONDITIONS_LIMITES("Conditions aux Limites");

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
     * Panel principal de visualisation adapté aux conditions générales
     */
    public static class PanelVisualisation extends JPanel {
        private Maillage maillage;
        private TypeVisualisation typeVisu;
        private AnalyseurErreurs.ResultatAnalyse analyseErreurs;
        private boolean afficherGrille = true;
        private boolean afficherValeurs = false;
        private boolean afficherConditionsLimites = true;
        private boolean mettreEnEvidenceBords = true;
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
                    dessinerColorationAvecConditions(g2d);
                    break;
                case COURBES_NIVEAU:
                    dessinerCourbesNiveauAvecConditions(g2d);
                    break;
                case ERREURS:
                    dessinerCarteErreurs(g2d);
                    break;
                case CONDITIONS_LIMITES:
                    dessinerVisualisationConditions(g2d);
                    break;
            }

            dessinerLegende(g2d);
            dessinerInformationsConditions(g2d);
        }

        /**
         * Dessine la représentation par coloration avec mise en évidence des conditions aux limites
         */
        private void dessinerColorationAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N = maillage.getN();

            // Calcul des dimensions d'affichage
            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge - 150;
            int hauteurDispo = getHeight() - 2 * marge - 80; // Plus d'espace pour les infos CL

            int tailleCellule = Math.min(largeurDispo / (N + 2), hauteurDispo / (N + 2));
            int offsetX = marge;
            int offsetY = marge;

            // Détermination des valeurs min/max pour la normalisation
            double[] minMax = calculerMinMax(U);
            double minVal = minMax[0];
            double maxVal = minMax[1];

            // Dessin du maillage coloré avec distinction des bords
            for (int i = 0; i <= N + 1; i++) {
                for (int j = 0; j <= N + 1; j++) {
                    int x = offsetX + j * tailleCellule;
                    int y = offsetY + (N + 1 - i) * tailleCellule;

                    Color couleurCellule;
                    if (maillage.isBoundary(i, j)) {
                        // Conditions aux limites avec couleur spéciale
                        if (mettreEnEvidenceBords) {
                            couleurCellule = getCouleurConditionLimite(U[i][j], minVal, maxVal);
                        } else {
                            double valeurNormalisee = normaliser(U[i][j], minVal, maxVal);
                            couleurCellule = getCouleurHeatmap(valeurNormalisee);
                        }
                    } else {
                        // Points intérieurs
                        double valeurNormalisee = normaliser(U[i][j], minVal, maxVal);
                        couleurCellule = getCouleurHeatmap(valeurNormalisee);
                    }

                    // Remplissage de la cellule
                    g2d.setColor(couleurCellule);
                    g2d.fillRect(x, y, tailleCellule, tailleCellule);

                    // Contour spécial pour les conditions aux limites
                    if (maillage.isBoundary(i, j) && mettreEnEvidenceBords) {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.drawRect(x, y, tailleCellule, tailleCellule);
                        g2d.setStroke(new BasicStroke(1));
                    } else if (afficherGrille) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.drawRect(x, y, tailleCellule, tailleCellule);
                    }

                    // Affichage des valeurs
                    if (afficherValeurs && tailleCellule > 25) {
                        g2d.setColor(maillage.isBoundary(i, j) ? Color.WHITE : Color.BLACK);
                        g2d.setFont(new Font("Arial", Font.PLAIN,
                            maillage.isBoundary(i, j) ? 9 : 8));
                        String valeurStr = String.format("%.2f", U[i][j]);
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = x + (tailleCellule - fm.stringWidth(valeurStr)) / 2;
                        int textY = y + (tailleCellule + fm.getAscent()) / 2;
                        g2d.drawString(valeurStr, textX, textY);
                    }
                }
            }

            // Dessin de la barre de couleur
            dessinerBarreCouleurAvecConditions(g2d, offsetX + (N + 2) * tailleCellule + 20, offsetY, minVal, maxVal);
        }

        /**
         * Dessine les courbes de niveau avec mise en évidence des conditions aux limites
         */
        private void dessinerCourbesNiveauAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N = maillage.getN();

            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge;
            int hauteurDispo = getHeight() - 2 * marge - 80;

            double echelleX = (double) largeurDispo / (N + 1);
            double echelleY = (double) hauteurDispo / (N + 1);

            // Fond blanc
            g2d.setColor(Color.WHITE);
            g2d.fillRect(marge, marge, largeurDispo, hauteurDispo);

            // Dessin du contour du domaine avec mise en évidence
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(marge, marge, largeurDispo, hauteurDispo);

            // Calcul et dessin des courbes de niveau
            if (niveauxContours != null) {
                Color[] couleursContours = genererCouleursContours(niveauxContours.length);

                for (int k = 0; k < niveauxContours.length; k++) {
                    double niveau = niveauxContours[k];
                    g2d.setColor(couleursContours[k]);
                    g2d.setStroke(new BasicStroke(1.5f));

                    // Algorithme de contours
                    for (int i = 1; i <= N; i++) {
                        for (int j = 1; j <= N; j++) {
                            dessinerContourCellule(g2d, U, i, j, niveau, marge, echelleX, echelleY, N);
                        }
                    }
                }
            }

            // Visualisation des conditions aux limites
            if (afficherConditionsLimites) {
                dessinerConditionsAuxLimitesSurContours(g2d, marge, echelleX, echelleY, N);
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
         * Dessine une représentation 3D avec mise en évidence des conditions aux limites
         */
        private void dessinerSurface3DAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N = maillage.getN();

            // Paramètres de projection isométrique
            double angleX = Math.PI / 6;
            double angleY = Math.PI / 4;
            double facteurZ = 50.0;

            int centreX = getWidth() / 2;
            int centreY = getHeight() / 2;
            int echelle = Math.min(getWidth(), getHeight()) / (N + 4);

            // Calcul des valeurs min/max
            double[] minMax = calculerMinMax(U);
            double minVal = minMax[0];
            double maxVal = minMax[1];
            double amplitude = maxVal - minVal;

            // Dessin de la surface avec distinction des bords
            for (int i = 0; i <= N + 1; i++) {
                for (int j = 0; j <= N + 1; j++) {
                    // Coordonnées 3D
                    double x1 = (j - (N+1)/2.0) * echelle / 2;
                    double y1 = (i - (N+1)/2.0) * echelle / 2;
                    double z1 = (U[i][j] - minVal) / amplitude * facteurZ;

                    // Projection isométrique
                    int screenX = (int) (centreX + x1 * Math.cos(angleY) + z1 * Math.sin(angleY));
                    int screenY = (int) (centreY - y1 * Math.cos(angleX) - z1 * Math.sin(angleX));

                    // Couleur et taille selon le type de point
                    if (maillage.isBoundary(i, j)) {
                        // Points des conditions aux limites
                        g2d.setColor(Color.RED);
                        g2d.fillOval(screenX - 3, screenY - 3, 6, 6);
                        g2d.setColor(Color.BLACK);
                        g2d.drawOval(screenX - 3, screenY - 3, 6, 6);
                    } else {
                        // Points intérieurs
                        double valeurNormalisee = (U[i][j] - minVal) / amplitude;
                        Color couleur = getCouleurHeatmap(valeurNormalisee);
                        g2d.setColor(couleur);
                        g2d.fillOval(screenX - 2, screenY - 2, 4, 4);
                    }

                    // Connexions pour former la surface
                    g2d.setColor(new Color(100, 100, 100, 150));
                    g2d.setStroke(new BasicStroke(1));

                    if (j < N + 1) {
                        double x2 = ((j+1) - (N+1)/2.0) * echelle / 2;
                        double z2 = (U[i][j+1] - minVal) / amplitude * facteurZ;
                        int screenX2 = (int) (centreX + x2 * Math.cos(angleY) + z2 * Math.sin(angleY));
                        int screenY2 = (int) (centreY - y1 * Math.cos(angleX) - z2 * Math.sin(angleX));
                        g2d.drawLine(screenX, screenY, screenX2, screenY2);
                    }

                    if (i < N + 1) {
                        double y2 = ((i+1) - (N+1)/2.0) * echelle / 2;
                        double z2 = (U[i+1][j] - minVal) / amplitude * facteurZ;
                        int screenX2 = (int) (centreX + x1 * Math.cos(angleY) + z2 * Math.sin(angleY));
                        int screenY2 = (int) (centreY - y2 * Math.cos(angleX) - z2 * Math.sin(angleX));
                        g2d.drawLine(screenX, screenY, screenX2, screenY2);
                    }
                }
            }
        }

        /**
         * Dessine une visualisation spécifique des conditions aux limites
         */
        private void dessinerVisualisationConditions(Graphics2D g2d) {
            if (maillage == null || maillage.getConditionsLimites() == null) {
                dessinerMessageAttente(g2d);
                return;
            }

            int N = maillage.getN();
            double h = maillage.getH();
            ConditionsLimites conditions = maillage.getConditionsLimites();

            int marge = 60;
            int largeur = getWidth() - 2 * marge;
            int hauteur = getHeight() - 2 * marge - 100;

            // Fond
            g2d.setColor(new Color(250, 250, 250));
            g2d.fillRect(marge, marge, largeur, hauteur);

            // Contour du domaine
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(marge, marge, largeur, hauteur);

            // Échantillonnage des conditions aux limites
            int nbEchantillons = 50;

            // Bord inférieur (y = 0)
            g2d.setColor(Color.BLUE);
            g2d.setStroke(new BasicStroke(4));
            for (int k = 0; k < nbEchantillons - 1; k++) {
                double t1 = (double) k / (nbEchantillons - 1);
                double t2 = (double) (k + 1) / (nbEchantillons - 1);

                double val1 = conditions.getValeurBord(0, (int)(t1 * (N + 1)), N, h);
                double val2 = conditions.getValeurBord(0, (int)(t2 * (N + 1)), N, h);

                int x1 = marge + (int) (t1 * largeur);
                int x2 = marge + (int) (t2 * largeur);
                int y1 = marge + hauteur - (int) (Math.abs(val1) * 20);
                int y2 = marge + hauteur - (int) (Math.abs(val2) * 20);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Bord supérieur (y = 1)
            g2d.setColor(Color.RED);
            for (int k = 0; k < nbEchantillons - 1; k++) {
                double t1 = (double) k / (nbEchantillons - 1);
                double t2 = (double) (k + 1) / (nbEchantillons - 1);

                double val1 = conditions.getValeurBord(N + 1, (int)(t1 * (N + 1)), N, h);
                double val2 = conditions.getValeurBord(N + 1, (int)(t2 * (N + 1)), N, h);

                int x1 = marge + (int) (t1 * largeur);
                int x2 = marge + (int) (t2 * largeur);
                int y1 = marge + (int) (Math.abs(val1) * 20);
                int y2 = marge + (int) (Math.abs(val2) * 20);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Bord gauche (x = 0)
            g2d.setColor(Color.GREEN);
            for (int k = 0; k < nbEchantillons - 1; k++) {
                double t1 = (double) k / (nbEchantillons - 1);
                double t2 = (double) (k + 1) / (nbEchantillons - 1);

                double val1 = conditions.getValeurBord((int)(t1 * (N + 1)), 0, N, h);
                double val2 = conditions.getValeurBord((int)(t2 * (N + 1)), 0, N, h);

                int y1 = marge + hauteur - (int) (t1 * hauteur);
                int y2 = marge + hauteur - (int) (t2 * hauteur);
                int x1 = marge + (int) (Math.abs(val1) * 20);
                int x2 = marge + (int) (Math.abs(val2) * 20);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Bord droit (x = 1)
            g2d.setColor(Color.ORANGE);
            for (int k = 0; k < nbEchantillons - 1; k++) {
                double t1 = (double) k / (nbEchantillons - 1);
                double t2 = (double) (k + 1) / (nbEchantillons - 1);

                double val1 = conditions.getValeurBord((int)(t1 * (N + 1)), N + 1, N, h);
                double val2 = conditions.getValeurBord((int)(t2 * (N + 1)), N + 1, N, h);

                int y1 = marge + hauteur - (int) (t1 * hauteur);
                int y2 = marge + hauteur - (int) (t2 * hauteur);
                int x1 = marge + largeur - (int) (Math.abs(val1) * 20);
                int x2 = marge + largeur - (int) (Math.abs(val2) * 20);

                g2d.drawLine(x1, y1, x2, y2);
            }

            // Légende des bords
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Visualisation des Conditions aux Limites", marge, marge - 20);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.setColor(Color.BLUE);
            g2d.drawString("Bord inférieur (y=0)", marge, getHeight() - 60);
            g2d.setColor(Color.RED);
            g2d.drawString("Bord supérieur (y=1)", marge, getHeight() - 45);
            g2d.setColor(Color.GREEN);
            g2d.drawString("Bord gauche (x=0)", marge, getHeight() - 30);
            g2d.setColor(Color.ORANGE);
            g2d.drawString("Bord droit (x=1)", marge, getHeight() - 15);
        }

        /**
         * Dessine les conditions aux limites sur les courbes de niveau
         */
        private void dessinerConditionsAuxLimitesSurContours(Graphics2D g2d, int marge,
                                                             double echelleX, double echelleY, int N) {
            double[][] U = maillage.getU();

            // Mise en évidence des valeurs aux bords
            g2d.setStroke(new BasicStroke(3));

            // Bord inférieur
            g2d.setColor(new Color(0, 0, 255, 150));
            for (int j = 0; j < N + 1; j++) {
                int x1 = (int) (marge + j * echelleX);
                int x2 = (int) (marge + (j + 1) * echelleX);
                int y = marge + (int) ((N + 1) * echelleY);
                g2d.drawLine(x1, y, x2, y);
            }

            // Bord supérieur
            g2d.setColor(new Color(255, 0, 0, 150));
            for (int j = 0; j < N + 1; j++) {
                int x1 = (int) (marge + j * echelleX);
                int x2 = (int) (marge + (j + 1) * echelleX);
                int y = marge;
                g2d.drawLine(x1, y, x2, y);
            }

            // Bords gauche et droit
            g2d.setColor(new Color(0, 255, 0, 150));
            for (int i = 0; i < N + 1; i++) {
                int y1 = (int) (marge + i * echelleY);
                int y2 = (int) (marge + (i + 1) * echelleY);
                g2d.drawLine(marge, y1, marge, y2);
                g2d.drawLine((int) (marge + (N + 1) * echelleX), y1,
                    (int) (marge + (N + 1) * echelleX), y2);
            }
        }

        /**
         * Calcule les niveaux pour les courbes de niveau
         */
        private void calculerNiveauxContours() {
            if (maillage == null) return;

            double[] minMax = calculerMinMax(maillage.getU());
            double minVal = minMax[0];
            double maxVal = minMax[1];

            int nbNiveaux = 12;
            niveauxContours = new double[nbNiveaux];

            for (int i = 0; i < nbNiveaux; i++) {
                niveauxContours[i] = minVal + (maxVal - minVal) * i / (nbNiveaux - 1);
            }
        }

        /**
         * Dessine un contour dans une cellule
         */
        private void dessinerContourCellule(Graphics2D g2d, double[][] U, int i, int j, double niveau,
                                            int marge, double echelleX, double echelleY, int N) {

            // Valeurs aux quatre coins
            double v00 = U[i][j];
            double v10 = U[i][j+1];
            double v01 = U[i+1][j];
            double v11 = U[i+1][j+1];

            List<Point2D.Double> intersections = new ArrayList<>();

            // Calcul des intersections (algorithme marching squares simplifié)
            // Bord gauche
            if ((v00 <= niveau && niveau <= v01) || (v01 <= niveau && niveau <= v00)) {
                if (Math.abs(v01 - v00) > 1e-10) {
                    double t = (niveau - v00) / (v01 - v00);
                    double x = marge + j * echelleX;
                    double y = marge + (N + 1 - i - t) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord droit
            if ((v10 <= niveau && niveau <= v11) || (v11 <= niveau && niveau <= v10)) {
                if (Math.abs(v11 - v10) > 1e-10) {
                    double t = (niveau - v10) / (v11 - v10);
                    double x = marge + (j + 1) * echelleX;
                    double y = marge + (N + 1 - i - t) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord bas
            if ((v00 <= niveau && niveau <= v10) || (v10 <= niveau && niveau <= v00)) {
                if (Math.abs(v10 - v00) > 1e-10) {
                    double t = (niveau - v00) / (v10 - v00);
                    double x = marge + (j + t) * echelleX;
                    double y = marge + (N + 1 - i) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Bord haut
            if ((v01 <= niveau && niveau <= v11) || (v11 <= niveau && niveau <= v01)) {
                if (Math.abs(v11 - v01) > 1e-10) {
                    double t = (niveau - v01) / (v11 - v01);
                    double x = marge + (j + t) * echelleX;
                    double y = marge + (N + 1 - i - 1) * echelleY;
                    intersections.add(new Point2D.Double(x, y));
                }
            }

            // Dessin des segments
            if (intersections.size() >= 2) {
                Point2D.Double p1 = intersections.get(0);
                Point2D.Double p2 = intersections.get(1);
                g2d.drawLine((int) p1.x, (int) p1.y, (int) p2.x, (int) p2.y);
            }
        }

        /**
         * Dessine la barre de couleur avec informations sur les conditions aux limites
         */
        private void dessinerBarreCouleurAvecConditions(Graphics2D g2d, int x, int y, double minVal, double maxVal) {
            int largeur = 25;
            int hauteur = 200;

            // Gradient principal
            for (int i = 0; i < hauteur; i++) {
                double valeurNormalisee = (double) i / hauteur;
                Color couleur = getCouleurHeatmap(valeurNormalisee);
                g2d.setColor(couleur);
                g2d.fillRect(x, y + hauteur - i, largeur, 1);
            }

            // Contour
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(x, y, largeur, hauteur);

            // Étiquettes des valeurs
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(String.format("%.3f", maxVal), x + largeur + 5, y + 10);
            g2d.drawString(String.format("%.3f", (maxVal + minVal) / 2), x + largeur + 5, y + hauteur / 2);
            g2d.drawString(String.format("%.3f", minVal), x + largeur + 5, y + hauteur + 5);

            // Indication spéciale pour les conditions aux limites
            if (mettreEnEvidenceBords) {
                g2d.setColor(Color.RED);
                g2d.fillRect(x + largeur + 2, y + hauteur + 20, 10, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x + largeur + 2, y + hauteur + 20, 10, 10);
                g2d.setFont(new Font("Arial", Font.PLAIN, 9));
                g2d.drawString("Conditions aux limites", x + largeur + 15, y + hauteur + 29);
            }
        }

        /**
         * Dessine les informations sur les conditions aux limites
         */
        private void dessinerInformationsConditions(Graphics2D g2d) {
            if (maillage == null) return;

            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));

            String info = String.format("Maillage: %dx%d, h=%.4f",
                maillage.getN() + 2, maillage.getN() + 2, maillage.getH());
            g2d.drawString(info, 10, getHeight() - 45);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String conditions = "CL: " + maillage.getConditionsLimites().getDescription();
            if (conditions.length() > 60) {
                conditions = conditions.substring(0, 57) + "...";
            }
            g2d.drawString(conditions, 10, getHeight() - 30);

            g2d.drawString("Type: " + typeVisu.getNom(), 10, getHeight() - 15);
        }

        /**
         * Dessine la carte des erreurs
         */
        private void dessinerCarteErreurs(Graphics2D g2d) {
            if (analyseErreurs == null || analyseErreurs.carteErreurs == null) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("Aucune analyse d'erreur disponible", 50, 50);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Résolvez d'abord un problème avec solution exacte", 50, 75);
                return;
            }

            double[][] erreurs = analyseErreurs.carteErreurs;
            int N = maillage.getN();

            int marge = 50;
            int tailleCellule = Math.min((getWidth() - 2 * marge - 150) / (N + 2),
                (getHeight() - 2 * marge - 50) / (N + 2));

            double[] minMax = calculerMinMax(erreurs);
            double maxErreur = minMax[1];

            for (int i = 0; i <= N + 1; i++) {
                for (int j = 0; j <= N + 1; j++) {
                    int x = marge + j * tailleCellule;
                    int y = marge + (N + 1 - i) * tailleCellule;

                    Color couleur;
                    if (maillage.isBoundary(i, j)) {
                        couleur = Color.DARK_GRAY; // CL en gris foncé
                    } else {
                        double erreurNormalisee = erreurs[i][j] / maxErreur;
                        couleur = getCouleurErreur(erreurNormalisee);
                    }

                    g2d.setColor(couleur);
                    g2d.fillRect(x, y, tailleCellule, tailleCellule);

                    g2d.setColor(Color.LIGHT_GRAY);
                    g2d.drawRect(x, y, tailleCellule, tailleCellule);
                }
            }

            // Légende pour les erreurs
            dessinerLegendErreurs(g2d, marge + (N + 2) * tailleCellule + 20, marge, maxErreur);
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
            // Légende spécifique selon le type de visualisation
            if (typeVisu == TypeVisualisation.CONDITIONS_LIMITES) {
                // Légende déjà incluse dans la visualisation
                return;
            }
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
         * Génère une couleur pour la heatmap
         */
        private Color getCouleurHeatmap(double valeurNormalisee) {
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
         * Génère une couleur spéciale pour les conditions aux limites
         */
        private Color getCouleurConditionLimite(double valeur, double minVal, double maxVal) {
            double valeurNormalisee = normaliser(valeur, minVal, maxVal);
            Color couleurBase = getCouleurHeatmap(valeurNormalisee);

            // Assombrir légèrement pour distinguer des points intérieurs
            int r = Math.max(0, couleurBase.getRed() - 30);
            int g = Math.max(0, couleurBase.getGreen() - 30);
            int b = Math.max(0, couleurBase.getBlue() - 30);

            return new Color(r, g, b);
        }

        /**
         * Génère une couleur pour la visualisation des erreurs
         */
        private Color getCouleurErreur(double valeurNormalisee) {
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

        // Getters et setters

        public void setAfficherGrille(boolean afficher) {
            this.afficherGrille = afficher;
            repaint();
        }

        public void setAfficherValeurs(boolean afficher) {
            this.afficherValeurs = afficher;
            repaint();
        }

        public void setAfficherConditionsLimites(boolean afficher) {
            this.afficherConditionsLimites = afficher;
            repaint();
        }

        public void setMettreEnEvidenceBords(boolean evidencer) {
            this.mettreEnEvidenceBords = evidencer;
            repaint();
        }

        public boolean isAfficherGrille() { return afficherGrille; }
        public boolean isAfficherValeurs() { return afficherValeurs; }
        public boolean isAfficherConditionsLimites() { return afficherConditionsLimites; }
        public boolean isMettreEnEvidenceBords() { return mettreEnEvidenceBords; }
    }

    /**
     * Crée une fenêtre de visualisation avec contrôles adaptés aux conditions générales
     */
    public static JFrame creerFenetreVisualisationAvecConditions(String titre, Maillage maillage,
                                                                 AnalyseurErreurs.ResultatAnalyse analyse,
                                                                 TypeVisualisation typeInitial) {
        JFrame fenetre = new JFrame(titre);
        fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        PanelVisualisation panel = new PanelVisualisation(900, 700);
        panel.setTypeVisualisation(typeInitial);
        panel.mettreAJour(maillage, analyse);

        // Panel de contrôles étendus
        JPanel controles = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Type de visualisation
        gbc.gridx = 0; gbc.gridy = 0;
        controles.add(new JLabel("Type:"), gbc);
        gbc.gridx = 1;
        JComboBox<TypeVisualisation> comboType = new JComboBox<>(TypeVisualisation.values());
        comboType.setSelectedItem(typeInitial);
        comboType.addActionListener(e -> {
            TypeVisualisation type = (TypeVisualisation) comboType.getSelectedItem();
            panel.setTypeVisualisation(type);
        });
        controles.add(comboType, gbc);

        // Options d'affichage
        gbc.gridx = 0; gbc.gridy = 1;
        JCheckBox checkGrille = new JCheckBox("Grille", panel.isAfficherGrille());
        checkGrille.addActionListener(e -> panel.setAfficherGrille(checkGrille.isSelected()));
        controles.add(checkGrille, gbc);

        gbc.gridx = 1;
        JCheckBox checkValeurs = new JCheckBox("Valeurs", panel.isAfficherValeurs());
        checkValeurs.addActionListener(e -> panel.setAfficherValeurs(checkValeurs.isSelected()));
        controles.add(checkValeurs, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JCheckBox checkConditions = new JCheckBox("Conditions aux limites", panel.isAfficherConditionsLimites());
        checkConditions.addActionListener(e -> panel.setAfficherConditionsLimites(checkConditions.isSelected()));
        controles.add(checkConditions, gbc);

        gbc.gridx = 1;
        JCheckBox checkEvidenceBords = new JCheckBox("Évidence bords", panel.isMettreEnEvidenceBords());
        checkEvidenceBords.addActionListener(e -> panel.setMettreEnEvidenceBords(checkEvidenceBords.isSelected()));
        controles.add(checkEvidenceBords, gbc);

        // Informations sur les conditions
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JTextArea infoConditions = new JTextArea(3, 40);
        infoConditions.setEditable(false);
        infoConditions.setBackground(new Color(240, 240, 240));
        infoConditions.setBorder(BorderFactory.createTitledBorder("Conditions aux Limites"));
        if (maillage != null && maillage.getConditionsLimites() != null) {
            infoConditions.setText(maillage.getConditionsLimites().getDescription());
            infoConditions.append("\nCompatibilité: " +
                (maillage.getConditionsLimites().verifierCompatibilite() ? "OK" : "Problème"));
        }
        controles.add(new JScrollPane(infoConditions), gbc);

        // Boutons d'action
        gbc.gridy = 4; gbc.gridwidth = 1;
        JButton boutonExporter = new JButton("Exporter Image");
        boutonExporter.addActionListener(e -> exporterImage(panel));
        controles.add(boutonExporter, gbc);

        gbc.gridx = 1;
        JButton boutonConditionsSeules = new JButton("Voir Conditions");
        boutonConditionsSeules.addActionListener(e -> {
            panel.setTypeVisualisation(TypeVisualisation.CONDITIONS_LIMITES);
            comboType.setSelectedItem(TypeVisualisation.CONDITIONS_LIMITES);
        });
        controles.add(boutonConditionsSeules, gbc);

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
     * Méthode utilitaire pour créer rapidement une visualisation avec conditions
     */
    public static void visualiserAvecConditions(Maillage maillage, AnalyseurErreurs.ResultatAnalyse analyse, String titre) {
        SwingUtilities.invokeLater(() -> {
            JFrame fenetre = creerFenetreVisualisationAvecConditions(titre, maillage, analyse, TypeVisualisation.COLORATION);
            fenetre.setVisible(true);
        });
    }

    /**
     * Crée un panel de comparaison pour différentes conditions aux limites
     */
    public static JPanel creerPanelComparaisonConditions(Maillage[] maillages, String[] titres,
                                                         AnalyseurErreurs.ResultatAnalyse[] analyses) {
        JPanel panelPrincipal = new JPanel(new GridLayout(2, 2, 10, 10));

        for (int i = 0; i < Math.min(4, maillages.length); i++) {
            JPanel panelIndividuel = new JPanel(new BorderLayout());

            PanelVisualisation panel = new PanelVisualisation(350, 350);
            panel.setMettreEnEvidenceBords(true); // Mettre en évidence les conditions
            panel.mettreAJour(maillages[i], analyses != null ? analyses[i] : null);

            // Titre avec informations sur les conditions
            JPanel panelTitre = new JPanel(new BorderLayout());
            JLabel titre = new JLabel(titres[i], JLabel.CENTER);
            titre.setFont(new Font("Arial", Font.BOLD, 12));

            JLabel conditions = new JLabel("CL: " + maillages[i].getConditionsLimites().getType().name(), JLabel.CENTER);
            conditions.setFont(new Font("Arial", Font.PLAIN, 10));
            conditions.setForeground(Color.BLUE);

            panelTitre.add(titre, BorderLayout.NORTH);
            panelTitre.add(conditions, BorderLayout.SOUTH);

            panelIndividuel.add(panelTitre, BorderLayout.NORTH);
            panelIndividuel.add(panel, BorderLayout.CENTER);
            panelIndividuel.setBorder(BorderFactory.createEtchedBorder());

            panelPrincipal.add(panelIndividuel);
        }

        return panelPrincipal;
    }

    /**
     * Crée une animation montrant l'évolution de la solution avec différentes conditions
     */
    public static JFrame creerAnimationConditions(Maillage[] maillagesSequence, String[] descriptions) {
        JFrame fenetre = new JFrame("Animation - Évolution avec Conditions aux Limites");
        fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fenetre.setSize(800, 700);

        PanelVisualisation panel = new PanelVisualisation(700, 600);
        panel.setMettreEnEvidenceBords(true);

        JPanel controles = new JPanel(new FlowLayout());

        JSlider slider = new JSlider(0, maillagesSequence.length - 1, 0);
        slider.addChangeListener(e -> {
            int index = slider.getValue();
            panel.mettreAJour(maillagesSequence[index], null);
        });

        JLabel labelDesc = new JLabel(descriptions[0]);
        labelDesc.setFont(new Font("Arial", Font.BOLD, 12));

        slider.addChangeListener(e -> {
            int index = slider.getValue();
            labelDesc.setText(descriptions[index]);
        });

        JButton boutonAuto = new JButton("Animation Auto");
        Timer timer = new Timer(1000, e -> {
            int val = slider.getValue();
            slider.setValue((val + 1) % maillagesSequence.length);
        });

        boutonAuto.addActionListener(e -> {
            if (timer.isRunning()) {
                timer.stop();
                boutonAuto.setText("Animation Auto");
            } else {
                timer.start();
                boutonAuto.setText("Arrêter");
            }
        });

        controles.add(new JLabel("Étape:"));
        controles.add(slider);
        controles.add(boutonAuto);

        JPanel panelInfo = new JPanel(new BorderLayout());
        panelInfo.add(labelDesc, BorderLayout.CENTER);
        panelInfo.setBorder(BorderFactory.createTitledBorder("Description"));

        fenetre.setLayout(new BorderLayout());
        fenetre.add(controles, BorderLayout.NORTH);
        fenetre.add(panel, BorderLayout.CENTER);
        fenetre.add(panelInfo, BorderLayout.SOUTH);

        // Initialiser avec le premier maillage
        panel.mettreAJour(maillagesSequence[0], null);

        fenetre.setLocationRelativeTo(null);
        return fenetre;
    }

    /**
     * Analyse comparative visuelle des conditions aux limites
     */
    public static void analyseVisuelleConditions(Maillage maillage) {
        // Créer différentes visualisations de la même solution
        String[] types = {"Coloration", "Contours", "3D", "Conditions"};
        TypeVisualisation[] typesVisu = {
            TypeVisualisation.COLORATION,
            TypeVisualisation.COURBES_NIVEAU,
            TypeVisualisation.CONDITIONS_LIMITES
        };

        JFrame fenetre = new JFrame("Analyse Visuelle - " + maillage.getConditionsLimites().getDescription());
        fenetre.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        fenetre.setSize(1200, 800);

        JPanel panelPrincipal = new JPanel(new GridLayout(2, 2, 10, 10));

        for (int i = 0; i < 4; i++) {
            JPanel panelIndividuel = new JPanel(new BorderLayout());

            PanelVisualisation panel = new PanelVisualisation(400, 350);
            panel.setTypeVisualisation(typesVisu[i]);
            panel.setMettreEnEvidenceBords(true);
            panel.mettreAJour(maillage, null);

            JLabel titre = new JLabel(types[i], JLabel.CENTER);
            titre.setFont(new Font("Arial", Font.BOLD, 14));
            titre.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

            panelIndividuel.add(titre, BorderLayout.NORTH);
            panelIndividuel.add(panel, BorderLayout.CENTER);
            panelIndividuel.setBorder(BorderFactory.createEtchedBorder());

            panelPrincipal.add(panelIndividuel);
        }

        // Informations globales
        JPanel panelInfo = new JPanel(new BorderLayout());
        JTextArea infoArea = new JTextArea(4, 0);
        infoArea.setEditable(false);
        infoArea.setText("Maillage: " + (maillage.getN() + 2) + "×" + (maillage.getN() + 2) + "\n" +
            "Conditions aux limites: " + maillage.getConditionsLimites().getDescription() + "\n" +
            "Compatibilité: " + (maillage.getConditionsLimites().verifierCompatibilite() ? "OK" : "Problème") + "\n" +
            "Cohérence du maillage: " + (maillage.verifierCoherence() ? "OK" : "Problème"));
        infoArea.setBackground(new Color(240, 240, 240));
        panelInfo.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        panelInfo.setBorder(BorderFactory.createTitledBorder("Informations"));

        fenetre.setLayout(new BorderLayout());
        fenetre.add(panelPrincipal, BorderLayout.CENTER);
        fenetre.add(panelInfo, BorderLayout.SOUTH);

        fenetre.setLocationRelativeTo(null);
        fenetre.setVisible(true);
    }
}
