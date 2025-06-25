import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe pour la visualisation graphique des solutions avec conditions aux limites générales
 *
 * Implémente plusieurs types de représentation :
 * 1. Système de coloration (heatmap) Représentation par couleurs
 * 2. Courbes de niveau (contours) Isolignes de la solution
 * 3. Surface 3D - Projection isométrique (Non implémenté dans cette version, mais dessinerSurface3DAvecConditions est là)
 * 4. Carte des erreurs - Visualisation des erreurs locales
 *
 * Adaptations pour les conditions aux limites générales :
 * - Affichage des valeurs des conditions aux limites
 * - Mise en évidence des variations aux bords
 * - Analyse de la continuité aux interfaces
 * - Visualisation de l'influence des conditions sur la solution
 * - Affichage des coordonnées et valeur au survol
 */
public class VisualiseurGraphique {

    /**
     * Énumération des types de visualisation disponibles
     */
    public enum TypeVisualisation {
        COLORATION("Système de Coloration (Heatmap)"),
        COURBES_NIVEAU("Courbes de Niveau (Contours)"),
        ERREURS("Carte des Erreurs"),
        CONDITIONS_LIMITES("Visualisation des Conditions aux Limites");

        private final String nom;
        TypeVisualisation(String nom) { this.nom = nom; }
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
        private boolean afficherConditionsLimitesTexte = true; // Renommé pour clarté
        private boolean mettreEnEvidenceBords = true;
        private double[] niveauxContours;

        // Variables pour le tooltip et le dessin, mises à jour dans paintComponent
        private int lastOffsetX, lastOffsetY, lastTailleCellule, lastNtotal;


        public PanelVisualisation(int largeur, int hauteur) {
            setPreferredSize(new Dimension(largeur, hauteur));
            setBackground(Color.WHITE);
            this.typeVisu = TypeVisualisation.COLORATION;

            // Activer les tooltips
            ToolTipManager.sharedInstance().registerComponent(this);

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (maillage == null || lastNtotal == 0 || lastTailleCellule == 0) {
                        setToolTipText(null);
                        return;
                    }

                    int mouseX = e.getX();
                    int mouseY = e.getY();

                    // Convertir les coordonnées de la souris en indices de cellule (i, j globaux)
                    // Attention à l'inversion de l'axe Y pour l'affichage
                    int j_grid = (mouseX - lastOffsetX) / lastTailleCellule;
                    int i_grid_visu = (mouseY - lastOffsetY) / lastTailleCellule; // Indice i pour affichage (0 en haut)
                    int i_grid = lastNtotal - 1 - i_grid_visu; // Indice i mathématique (0 en bas)


                    if (j_grid >= 0 && j_grid < lastNtotal && i_grid >= 0 && i_grid < lastNtotal) {
                        double valeur = maillage.getU(i_grid, j_grid);
                        double[] coords = maillage.getCoordinates(i_grid, j_grid); // x, y physiques
                        String tooltip = String.format("Cellule [%d,%d] Coords (%.3f, %.3f) Valeur: %.4e",
                            i_grid, j_grid, coords[0], coords[1], valeur);
                        if (maillage.isBoundary(i_grid,j_grid)) {
                            tooltip += " (Bord)";
                        } else {
                            tooltip += " (Intérieur)";
                        }
                        setToolTipText(tooltip);
                    } else {
                        setToolTipText(null); // Souris en dehors de la grille
                    }
                }
            });
        }

        /**
         * Met à jour les données à visualiser
         */
        public void mettreAJour(Maillage maillage, AnalyseurErreurs.ResultatAnalyse analyseErreurs) {
            this.maillage = maillage;
            this.analyseErreurs = analyseErreurs;
            if (maillage != null) {
                calculerNiveauxContours();
            }
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

            if (maillage == null || maillage.getN_total() == 0) {
                dessinerMessageAttente(g);
                return;
            }

            lastNtotal = maillage.getN_total(); // Pour le tooltip

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

            dessinerLegende(g2d); // Peut-être redondant si barre de couleur déjà là
            if (afficherConditionsLimitesTexte) { // Afficher le texte des conditions
                dessinerInformationsConditions(g2d);
            }
        }

        /**
         * Dessine la représentation par coloration avec mise en évidence des conditions aux limites
         */
        private void dessinerColorationAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N_total = maillage.getN_total(); // Nombre total de points

            // Calcul des dimensions d'affichage
            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge - 150; // Espace pour barre de couleur
            int hauteurDispo = getHeight() - 2 * marge - 80; // Espace pour infos

            // N_total cellules à afficher
            int tailleCellule = (N_total > 0) ? Math.min(largeurDispo / N_total, hauteurDispo / N_total) : 10;
            tailleCellule = Math.max(1, tailleCellule); // Assurer une taille minimale

            int offsetX = marge;
            int offsetY = marge;

            // Mettre à jour pour le tooltip
            this.lastOffsetX = offsetX;
            this.lastOffsetY = offsetY;
            this.lastTailleCellule = tailleCellule;


            double[] minMax = calculerMinMax(U);
            double minVal = minMax[0];
            double maxVal = minMax[1];

            // Dessin du maillage coloré
            // i, j sont les indices GLOBAUX du maillage (0 à N_total-1)
            for (int i = 0; i < N_total; i++) {
                for (int j = 0; j < N_total; j++) {
                    // x, y sont les coins supérieurs gauches des cellules à dessiner
                    // L'axe y de l'affichage est inversé par rapport à l'axe y mathématique
                    int x_rect = offsetX + j * tailleCellule;
                    int y_rect = offsetY + (N_total - 1 - i) * tailleCellule; // Inversion de l'axe i

                    Color couleurCellule;
                    if (maillage.isBoundary(i, j)) {
                        if (mettreEnEvidenceBords) {
                            couleurCellule = getCouleurConditionLimite(U[i][j], minVal, maxVal);
                        } else {
                            double valeurNormalisee = normaliser(U[i][j], minVal, maxVal);
                            couleurCellule = getCouleurHeatmap(valeurNormalisee);
                        }
                    } else {
                        double valeurNormalisee = normaliser(U[i][j], minVal, maxVal);
                        couleurCellule = getCouleurHeatmap(valeurNormalisee);
                    }

                    g2d.setColor(couleurCellule);
                    g2d.fillRect(x_rect, y_rect, tailleCellule, tailleCellule);

                    if (maillage.isBoundary(i, j) && mettreEnEvidenceBords) {
                        g2d.setColor(Color.BLACK);
                        g2d.setStroke(new BasicStroke(2));
                        g2d.drawRect(x_rect, y_rect, tailleCellule, tailleCellule);
                        g2d.setStroke(new BasicStroke(1));
                    } else if (afficherGrille) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.drawRect(x_rect, y_rect, tailleCellule, tailleCellule);
                    }

                    if (afficherValeurs && tailleCellule > 25) {
                        g2d.setColor(maillage.isBoundary(i, j) ? Color.WHITE : Color.BLACK);
                        g2d.setFont(new Font("Arial", Font.PLAIN, maillage.isBoundary(i,j) ? 9 : 8));
                        String valeurStr = String.format("%.2f", U[i][j]);
                        FontMetrics fm = g2d.getFontMetrics();
                        int textX = x_rect + (tailleCellule - fm.stringWidth(valeurStr)) / 2;
                        int textY = y_rect + (tailleCellule + fm.getAscent()) / 2;
                        g2d.drawString(valeurStr, textX, textY);
                    }
                }
            }
            dessinerBarreCouleurAvecConditions(g2d, offsetX + N_total * tailleCellule + 20, offsetY, minVal, maxVal);
        }

        /**
         * Dessine les courbes de niveau avec mise en évidence des conditions aux limites
         */
        private void dessinerCourbesNiveauAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N_total = maillage.getN_total();
            if (N_total < 2) { // Pas assez de points pour des cellules
                g2d.drawString("Maillage trop petit pour les courbes de niveau", 50,50);
                return;
            }


            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge - 150; // Espace pour légende potentielle
            int hauteurDispo = getHeight() - 2 * marge - 80;

            // N_total-1 intervalles dans chaque direction
            double echelleX = (N_total > 1) ? (double) largeurDispo / (N_total - 1) : largeurDispo;
            double echelleY = (N_total > 1) ? (double) hauteurDispo / (N_total - 1) : hauteurDispo;

            // Mettre à jour pour le tooltip (approximatif pour courbes de niveau)
            // On utilise N_total-1 car echelleX/Y est par intervalle
            this.lastOffsetX = marge;
            this.lastOffsetY = marge;
            this.lastTailleCellule = (int)Math.min(echelleX, echelleY); // Approximation
            // this.lastNtotal reste maillage.getN_total()

            g2d.setColor(Color.WHITE);
            g2d.fillRect(marge, marge, largeurDispo, hauteurDispo);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawRect(marge, marge, largeurDispo, hauteurDispo);

            if (niveauxContours != null) {
                Color[] couleursContours = genererCouleursContours(niveauxContours.length);
                for (int k = 0; k < niveauxContours.length; k++) {
                    double niveau = niveauxContours[k];
                    g2d.setColor(couleursContours[k]);
                    g2d.setStroke(new BasicStroke(1.5f));
                    // Boucle sur les cellules (N_total-1 x N_total-1 cellules)
                    // i, j sont les indices du coin inférieur gauche de la cellule
                    for (int i_cell = 0; i_cell < N_total - 1; i_cell++) { // Indice global i du coin bas-gauche
                        for (int j_cell = 0; j_cell < N_total - 1; j_cell++) { // Indice global j du coin bas-gauche
                            dessinerContourCellule(g2d, U, i_cell, j_cell, niveau, marge, echelleX, echelleY, N_total);
                        }
                    }
                }
            }

            if (mettreEnEvidenceBords) { // Option pour visualiser les bords différemment
                dessinerConditionsAuxLimitesSurContours(g2d, marge, echelleX, echelleY, N_total);
            }

            if (afficherGrille && N_total > 1) {
                g2d.setColor(new Color(200, 200, 200, 100));
                g2d.setStroke(new BasicStroke(0.5f));
                for (int k = 0; k < N_total; k++) { // k itère sur les lignes de la grille
                    int x_ligne = (int) (marge + k * echelleX);
                    int y_ligne = (int) (marge + k * echelleY);
                    g2d.drawLine(x_ligne, marge, x_ligne, marge + hauteurDispo); // Lignes verticales
                    g2d.drawLine(marge, y_ligne, marge + largeurDispo, y_ligne); // Lignes horizontales
                }
            }
        }

        /**
         * Dessine une représentation 3D avec mise en évidence des conditions aux limites
         * (Adaptation de l'original, pourrait nécessiter plus de travail pour être parfait)
         */
        private void dessinerSurface3DAvecConditions(Graphics2D g2d) {
            double[][] U = maillage.getU();
            int N_total = maillage.getN_total();
            if (N_total == 0) return;

            double angleX = Math.PI / 6;
            double angleY = Math.PI / 4;
            double facteurZ = 50.0;

            int centreX = getWidth() / 2;
            int centreY = getHeight() / 2 + 50; // Descendre un peu pour le titre
            int echelle = Math.min(getWidth(), getHeight()) / (N_total + 2); // Un peu plus petit

            double[] minMax = calculerMinMax(U);
            double minVal = minMax[0];
            double maxVal = minMax[1];
            double amplitude = (Math.abs(maxVal - minVal) < 1e-9) ? 1.0 : (maxVal - minVal);


            Point[][] pointsProjetes = new Point[N_total][N_total];

            // Projeter tous les points
            for (int i = 0; i < N_total; i++) {
                for (int j = 0; j < N_total; j++) {
                    double x_norm = (j - (N_total - 1) / 2.0) * echelle; // Coordonnées centrées
                    double y_norm = (i - (N_total - 1) / 2.0) * echelle;
                    double z_norm = (U[i][j] - minVal) / amplitude * facteurZ;

                    int screenX = (int) (centreX + x_norm * Math.cos(angleY) - y_norm * Math.sin(angleY));
                    int screenY = (int) (centreY - x_norm * Math.sin(angleY) * Math.sin(angleX)
                        - y_norm * Math.cos(angleY) * Math.sin(angleX)
                        + z_norm * Math.cos(angleX));
                    pointsProjetes[i][j] = new Point(screenX, screenY);
                }
            }

            // Dessiner les polygones (de l'arrière vers l'avant pour un effet de masquage simple)
            for (int i = N_total - 2; i >= 0; i--) {
                for (int j = N_total - 2; j >= 0; j--) {
                    Path2D path = new Path2D.Double();
                    path.moveTo(pointsProjetes[i][j].x, pointsProjetes[i][j].y);
                    path.lineTo(pointsProjetes[i][j+1].x, pointsProjetes[i][j+1].y);
                    path.lineTo(pointsProjetes[i+1][j+1].x, pointsProjetes[i+1][j+1].y);
                    path.lineTo(pointsProjetes[i+1][j].x, pointsProjetes[i+1][j].y);
                    path.closePath();

                    // Couleur basée sur la valeur moyenne de la cellule
                    double valMoyenneCellule = (U[i][j] + U[i+1][j] + U[i][j+1] + U[i+1][j+1]) / 4.0;
                    double valNorm = normaliser(valMoyenneCellule, minVal, maxVal);
                    g2d.setColor(getCouleurHeatmap(valNorm));
                    g2d.fill(path);
                    g2d.setColor(Color.DARK_GRAY);
                    g2d.draw(path);
                }
            }

            // Dessiner les points des conditions aux limites en évidence
            for (int i = 0; i < N_total; i++) {
                for (int j = 0; j < N_total; j++) {
                    if (maillage.isBoundary(i,j) && mettreEnEvidenceBords) {
                        g2d.setColor(Color.RED);
                        g2d.fillOval(pointsProjetes[i][j].x - 3, pointsProjetes[i][j].y - 3, 6, 6);
                    }
                }
            }
        }


        /**
         * Dessine une visualisation spécifique des conditions aux limites
         */
        private void dessinerVisualisationConditions(Graphics2D g2d) {
            if (maillage == null || maillage.getConditionsLimites() == null || maillage.getN_total() < 2) {
                dessinerMessageAttente(g2d);
                return;
            }

            int N_total = maillage.getN_total();
            double h = maillage.getH();
            ConditionsLimites conditions = maillage.getConditionsLimites();

            int marge = 60;
            int largeurPlot = getWidth() - 2 * marge;
            int hauteurPlot = getHeight() - 2 * marge - 100; // Espace pour légende en bas

            g2d.setColor(new Color(250, 250, 250));
            g2d.fillRect(marge, marge, largeurPlot, hauteurPlot);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(marge, marge, largeurPlot, hauteurPlot);

            // Trouver min/max des valeurs de CL pour normaliser l'affichage graphique
            double minCL = Double.MAX_VALUE;
            double maxCL = Double.MIN_VALUE;
            for(int k=0; k < N_total; k++){
                minCL = Math.min(minCL, conditions.getValeurBord(0, k, N_total, h));
                maxCL = Math.max(maxCL, conditions.getValeurBord(0, k, N_total, h));
                minCL = Math.min(minCL, conditions.getValeurBord(N_total-1, k, N_total, h));
                maxCL = Math.max(maxCL, conditions.getValeurBord(N_total-1, k, N_total, h));
                minCL = Math.min(minCL, conditions.getValeurBord(k, 0, N_total, h));
                maxCL = Math.max(maxCL, conditions.getValeurBord(k, 0, N_total, h));
                minCL = Math.min(minCL, conditions.getValeurBord(k, N_total-1, N_total, h));
                maxCL = Math.max(maxCL, conditions.getValeurBord(k, N_total-1, N_total, h));
            }
            if (Math.abs(maxCL - minCL) < 1e-9) { // Éviter division par zéro si CL constantes
                maxCL = minCL + 1.0;
            }

            double plotHeightRange = hauteurPlot / 4.0; // Hauteur pour la variation des CL

            // Bord inférieur (y=0), dessiné en bas du rectangle de visualisation
            g2d.setColor(Color.BLUE); g2d.setStroke(new BasicStroke(2));
            Path2D pathInf = new Path2D.Double();
            pathInf.moveTo(marge, marge + hauteurPlot);
            for (int j_idx = 0; j_idx < N_total; j_idx++) {
                double x_coord = j_idx * h; // de 0 à 1
                double val = conditions.getValeurBord(0, j_idx, N_total, h);
                double y_plot = marge + hauteurPlot - normaliser(val, minCL, maxCL) * plotHeightRange;
                if (j_idx == 0) pathInf.moveTo(marge + x_coord * largeurPlot, y_plot);
                else pathInf.lineTo(marge + x_coord * largeurPlot, y_plot);
            }
            g2d.draw(pathInf);

            // Bord supérieur (y=1), dessiné en haut
            g2d.setColor(Color.RED);
            Path2D pathSup = new Path2D.Double();
            pathSup.moveTo(marge, marge);
            for (int j_idx = 0; j_idx < N_total; j_idx++) {
                double x_coord = j_idx * h;
                double val = conditions.getValeurBord(N_total - 1, j_idx, N_total, h);
                double y_plot = marge + normaliser(val, minCL, maxCL) * plotHeightRange;
                if (j_idx == 0) pathSup.moveTo(marge + x_coord * largeurPlot, y_plot);
                else pathSup.lineTo(marge + x_coord * largeurPlot, y_plot);
            }
            g2d.draw(pathSup);

            // Bord gauche (x=0), dessiné à gauche
            g2d.setColor(Color.GREEN.darker());
            Path2D pathGauche = new Path2D.Double();
            pathGauche.moveTo(marge, marge + hauteurPlot);
            for (int i_idx = 0; i_idx < N_total; i_idx++) {
                double y_coord = i_idx * h; // de 0 à 1
                double val = conditions.getValeurBord(i_idx, 0, N_total, h);
                double x_plot = marge + normaliser(val, minCL, maxCL) * (largeurPlot / 4.0) ; // Variation horizontale
                if (i_idx == 0) pathGauche.moveTo(x_plot, marge + hauteurPlot - y_coord * hauteurPlot);
                else pathGauche.lineTo(x_plot, marge + hauteurPlot - y_coord * hauteurPlot);
            }
            g2d.draw(pathGauche);

            // Bord droit (x=1), dessiné à droite
            g2d.setColor(Color.ORANGE.darker());
            Path2D pathDroit = new Path2D.Double();
            pathDroit.moveTo(marge + largeurPlot, marge + hauteurPlot);
            for (int i_idx = 0; i_idx < N_total; i_idx++) {
                double y_coord = i_idx * h;
                double val = conditions.getValeurBord(i_idx, N_total - 1, N_total, h);
                double x_plot = marge + largeurPlot - normaliser(val, minCL, maxCL) * (largeurPlot/4.0);
                if (i_idx == 0) pathDroit.moveTo(x_plot, marge + hauteurPlot - y_coord * hauteurPlot);
                else pathDroit.lineTo(x_plot, marge + hauteurPlot - y_coord * hauteurPlot);
            }
            g2d.draw(pathDroit);


            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString("Visualisation des Conditions aux Limites", marge, marge - 20);
            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            int legendeY = getHeight() - 75;
            g2d.setColor(Color.BLUE); g2d.drawString("Bord inférieur (y=0)", marge, legendeY);
            g2d.setColor(Color.RED); g2d.drawString("Bord supérieur (y=1)", marge, legendeY + 15);
            g2d.setColor(Color.GREEN.darker()); g2d.drawString("Bord gauche (x=0)", marge, legendeY + 30);
            g2d.setColor(Color.ORANGE.darker()); g2d.drawString("Bord droit (x=1)", marge, legendeY + 45);
            g2d.setColor(Color.BLACK);
            g2d.drawString(String.format("Plage CL: [%.2f, %.2f]", minCL, maxCL), marge + 150, legendeY);
        }


        /**
         * Dessine les conditions aux limites sur les courbes de niveau
         */
        private void dessinerConditionsAuxLimitesSurContours(Graphics2D g2d, int marge,
                                                             double echelleX, double echelleY, int N_total) {
            if (N_total < 2) return;
            // Met en évidence les segments du bord du domaine
            g2d.setStroke(new BasicStroke(3f));

            // Bord inférieur (i=0)
            g2d.setColor(new Color(0, 0, 255, 150)); // Bleu semi-transparent
            for (int j_seg = 0; j_seg < N_total - 1; j_seg++) { // N_total-1 segments
                int x1 = (int) (marge + j_seg * echelleX);
                int x2 = (int) (marge + (j_seg + 1) * echelleX);
                int y_plot = marge + (int) ((N_total - 1) * echelleY); // Bord du bas en coords d'affichage
                g2d.drawLine(x1, y_plot, x2, y_plot);
            }

            // Bord supérieur (i=N_total-1)
            g2d.setColor(new Color(255, 0, 0, 150)); // Rouge semi-transparent
            for (int j_seg = 0; j_seg < N_total - 1; j_seg++) {
                int x1 = (int) (marge + j_seg * echelleX);
                int x2 = (int) (marge + (j_seg + 1) * echelleX);
                int y_plot = marge; // Bord du haut en coords d'affichage
                g2d.drawLine(x1, y_plot, x2, y_plot);
            }

            // Bord gauche (j=0)
            g2d.setColor(new Color(0, 255, 0, 150)); // Vert semi-transparent
            for (int i_seg = 0; i_seg < N_total - 1; i_seg++) {
                int y1_plot = (int) (marge + i_seg * echelleY); // Inversion Y gérée par dessinContourCellule
                int y2_plot = (int) (marge + (i_seg + 1) * echelleY);
                int x_plot = marge;
                g2d.drawLine(x_plot, y1_plot, x_plot, y2_plot);
            }

            // Bord droit (j=N_total-1)
            // g2d.setColor(new Color(255,165,0,150)); // Orange semi-transparent (ou garder vert)
            for (int i_seg = 0; i_seg < N_total - 1; i_seg++) {
                int y1_plot = (int) (marge + i_seg * echelleY);
                int y2_plot = (int) (marge + (i_seg + 1) * echelleY);
                int x_plot = (int) (marge + (N_total - 1) * echelleX);
                g2d.drawLine(x_plot, y1_plot, x_plot, y2_plot);
            }
        }


        /**
         * Calcule les niveaux pour les courbes de niveau
         */
        private void calculerNiveauxContours() {
            if (maillage == null || maillage.getU() == null) return;
            double[] minMax = calculerMinMax(maillage.getU());
            double minVal = minMax[0];
            double maxVal = minMax[1];

            if (Math.abs(maxVal - minVal) < 1e-9) { // Si la solution est constante
                niveauxContours = new double[]{minVal};
                return;
            }

            int nbNiveaux = 12; // Nombre de courbes de niveau
            niveauxContours = new double[nbNiveaux];
            for (int i = 0; i < nbNiveaux; i++) {
                // Évite les niveaux exacts min/max pour une meilleure visibilité
                niveauxContours[i] = minVal + (maxVal - minVal) * (i + 0.5) / nbNiveaux;
            }
        }


        /**
         * Dessine un contour dans une cellule (algorithme Marching Squares simplifié)
         * @param U matrice des valeurs
         * @param i_bl, j_bl indices globaux du coin inférieur gauche (bottom-left) de la cellule
         * @param niveau valeur de l'isocourbe
         * @param N_total taille totale du maillage
         */
        private void dessinerContourCellule(Graphics2D g2d, double[][] U, int i_bl, int j_bl, double niveau,
                                            int marge, double echelleX, double echelleY, int N_total) {
            // Coins de la cellule (bottom-left, bottom-right, top-left, top-right)
            // Indices globaux :
            // (i_bl, j_bl) (i_bl, j_bl+1)
            // (i_bl+1, j_bl) (i_bl+1, j_bl+1)
            double v_bl = U[i_bl][j_bl];         // Valeur au coin inférieur gauche
            double v_br = U[i_bl][j_bl+1];       // Valeur au coin inférieur droit
            double v_tl = U[i_bl+1][j_bl];       // Valeur au coin supérieur gauche
            double v_tr = U[i_bl+1][j_bl+1];     // Valeur au coin supérieur droit

            // Coordonnées d'affichage des coins de la cellule (y est inversé)
            // Coin inférieur gauche de la cellule (i_bl, j_bl)
            double x_base = marge + j_bl * echelleX;
            double y_base_math = i_bl * echelleY; // y mathématique (0 en bas)
            double y_base_display = marge + (N_total - 1 - i_bl) * echelleY; // y affichage (0 en haut) pour le coin BAS de la cellule

            List<Point2D.Double> intersections = new ArrayList<>();

            // Segment gauche : entre (i_bl, j_bl) et (i_bl+1, j_bl)
            if ((v_bl <= niveau && niveau < v_tl) || (v_tl <= niveau && niveau < v_bl)) {
                if (Math.abs(v_tl - v_bl) > 1e-10) {
                    double t = (niveau - v_bl) / (v_tl - v_bl); // t va de 0 (v_bl) à 1 (v_tl)
                    intersections.add(new Point2D.Double(x_base, y_base_display - t * echelleY));
                }
            }
            // Segment droit : entre (i_bl, j_bl+1) et (i_bl+1, j_bl+1)
            if ((v_br <= niveau && niveau < v_tr) || (v_tr <= niveau && niveau < v_br)) {
                if (Math.abs(v_tr - v_br) > 1e-10) {
                    double t = (niveau - v_br) / (v_tr - v_br);
                    intersections.add(new Point2D.Double(x_base + echelleX, y_base_display - t * echelleY));
                }
            }
            // Segment bas : entre (i_bl, j_bl) et (i_bl, j_bl+1)
            if ((v_bl <= niveau && niveau < v_br) || (v_br <= niveau && niveau < v_bl)) {
                if (Math.abs(v_br - v_bl) > 1e-10) {
                    double t = (niveau - v_bl) / (v_br - v_bl);
                    intersections.add(new Point2D.Double(x_base + t * echelleX, y_base_display));
                }
            }
            // Segment haut : entre (i_bl+1, j_bl) et (i_bl+1, j_bl+1)
            if ((v_tl <= niveau && niveau < v_tr) || (v_tr <= niveau && niveau < v_tl)) {
                if (Math.abs(v_tr - v_tl) > 1e-10) {
                    double t = (niveau - v_tl) / (v_tr - v_tl);
                    intersections.add(new Point2D.Double(x_base + t * echelleX, y_base_display - echelleY));
                }
            }

            if (intersections.size() == 2) {
                g2d.draw(new Line2D.Double(intersections.get(0), intersections.get(1)));
            } else if (intersections.size() == 4) { // Cas ambigus, on joint arbitrairement par paires
                g2d.draw(new Line2D.Double(intersections.get(0), intersections.get(1)));
                g2d.draw(new Line2D.Double(intersections.get(2), intersections.get(3)));
            }
        }


        /**
         * Dessine la barre de couleur avec informations sur les conditions aux limites
         */
        private void dessinerBarreCouleurAvecConditions(Graphics2D g2d, int x, int y, double minVal, double maxVal) {
            int largeur = 25;
            int hauteur = 200;

            for (int i = 0; i < hauteur; i++) {
                double valeurNormalisee = (double) i / hauteur; // 0 en bas, 1 en haut
                Color couleur = getCouleurHeatmap(valeurNormalisee);
                g2d.setColor(couleur);
                g2d.fillRect(x, y + hauteur - 1 - i, largeur, 1); // y + hauteur -1 - i pour dessiner de bas en haut
            }

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1)); // Moins épais
            g2d.drawRect(x, y, largeur, hauteur);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(String.format("%.3f", maxVal), x + largeur + 5, y + 10);
            g2d.drawString(String.format("%.3f", (maxVal + minVal) / 2), x + largeur + 5, y + hauteur / 2 + 5);
            g2d.drawString(String.format("%.3f", minVal), x + largeur + 5, y + hauteur);

            if (mettreEnEvidenceBords && typeVisu == TypeVisualisation.COLORATION) { // Uniquement pour coloration
                Color clExampleColor = getCouleurConditionLimite(minVal + (maxVal-minVal)*0.75, minVal, maxVal); // Exemple
                g2d.setColor(clExampleColor);
                g2d.fillRect(x, y + hauteur + 10, largeur, 10);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y + hauteur + 10, largeur, 10);
                g2d.drawString("Bords", x + largeur + 5, y + hauteur + 19);
            }
        }


        /**
         * Dessine les informations sur les conditions aux limites
         */
        private void dessinerInformationsConditions(Graphics2D g2d) {
            if (maillage == null) return;
            g2d.setColor(Color.BLACK);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            String info = String.format("Maillage: %dx%d (N_total), h=%.4f",
                maillage.getN_total(), maillage.getN_total(), maillage.getH());
            g2d.drawString(info, 10, getHeight() - 45);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            String conditions = "CL: " + maillage.getConditionsLimites().getDescription();
            if (conditions.length() > 70) { // Adapter si besoin
                conditions = conditions.substring(0, 67) + "...";
            }
            g2d.drawString(conditions, 10, getHeight() - 30);
            g2d.drawString("Type Visu: " + typeVisu.getNom(), 10, getHeight() - 15);
        }

        /**
         * Dessine la carte des erreurs
         */
        private void dessinerCarteErreurs(Graphics2D g2d) {
            if (analyseErreurs == null || analyseErreurs.carteErreurs == null || !analyseErreurs.solutionExacteDisponible) {
                g2d.setColor(Color.RED);
                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("Carte des erreurs non disponible.", 50, 50);
                g2d.setFont(new Font("Arial", Font.PLAIN, 12));
                g2d.drawString("Vérifiez que la solution exacte est calculée et que l'analyse a été faite.", 50, 75);
                return;
            }

            double[][] erreurs = analyseErreurs.carteErreurs; // Taille N_total x N_total
            int N_total = maillage.getN_total();

            int marge = 50;
            int largeurDispo = getWidth() - 2 * marge - 150; // Espace pour barre de couleur
            int hauteurDispo = getHeight() - 2 * marge - 50;

            int tailleCellule = (N_total > 0) ? Math.min(largeurDispo / N_total, hauteurDispo / N_total) : 10;
            tailleCellule = Math.max(1, tailleCellule);

            int offsetX = marge;
            int offsetY = marge;

            // Mettre à jour pour le tooltip (montrera les erreurs ici)
            this.lastOffsetX = offsetX;
            this.lastOffsetY = offsetY;
            this.lastTailleCellule = tailleCellule;
            // this.lastNtotal est déjà à jour


            double[] minMaxErreur = calculerMinMax(erreurs); // 0 à maxErreur
            double maxErreur = minMaxErreur[1];
            if (maxErreur < 1e-12) maxErreur = 1e-12; // Pour éviter division par zéro si erreur nulle

            for (int i = 0; i < N_total; i++) { // Indice global i
                for (int j = 0; j < N_total; j++) { // Indice global j
                    int x_rect = offsetX + j * tailleCellule;
                    int y_rect = offsetY + (N_total - 1 - i) * tailleCellule; // Inversion axe i

                    Color couleur;
                    if (maillage.isBoundary(i, j)) {
                        couleur = Color.DARK_GRAY; // Erreur sur CL généralement nulle ou non pertinente
                    } else {
                        double erreurNormalisee = erreurs[i][j] / maxErreur;
                        couleur = getCouleurErreur(erreurNormalisee);
                    }
                    g2d.setColor(couleur);
                    g2d.fillRect(x_rect, y_rect, tailleCellule, tailleCellule);

                    if (afficherGrille) {
                        g2d.setColor(Color.LIGHT_GRAY);
                        g2d.drawRect(x_rect, y_rect, tailleCellule, tailleCellule);
                    }
                }
            }
            dessinerLegendeErreurs(g2d, offsetX + N_total * tailleCellule + 20, offsetY, maxErreur);
        }


        /**
         * Dessine la légende pour les erreurs
         */
        private void dessinerLegendeErreurs(Graphics2D g2d, int x, int y, double maxErreur) {
            int largeur = 20;
            int hauteur = 200;

            for (int i = 0; i < hauteur; i++) {
                double valeurNormalisee = (double) i / hauteur; // 0 en bas, 1 en haut
                Color couleur = getCouleurErreur(valeurNormalisee);
                g2d.setColor(couleur);
                g2d.fillRect(x, y + hauteur - 1 - i, largeur, 1);
            }
            g2d.setColor(Color.BLACK);
            g2d.drawRect(x, y, largeur, hauteur);

            g2d.setFont(new Font("Arial", Font.PLAIN, 10));
            g2d.drawString(String.format("%.2e", maxErreur), x + largeur + 5, y + 10);
            g2d.drawString(String.format("%.2e", maxErreur / 2), x + largeur + 5, y + hauteur / 2 + 5);
            g2d.drawString("0.0", x + largeur + 5, y + hauteur);
        }


        /**
         * Dessine un message d'attente
         */
        private void dessinerMessageAttente(Graphics g) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.ITALIC, 16));
            String message = "En attente de données (Maillage non initialisé ou N_total=0)...";
            FontMetrics fm = g.getFontMetrics();
            int x_msg = (getWidth() - fm.stringWidth(message)) / 2;
            int y_msg = getHeight() / 2;
            g.drawString(message, x_msg, y_msg);
        }

        /**
         * Dessine une légende générale (peut être vide si non nécessaire)
         */
        private void dessinerLegende(Graphics2D g2d) {
            // Pas de légende générale pour l'instant, les barres de couleurs suffisent.
        }


        // Méthodes utilitaires

        /**
         * Calcule les valeurs min et max d'une matrice
         */
        private double[] calculerMinMax(double[][] matrice) {
            if (matrice == null || matrice.length == 0 || matrice[0].length == 0) {
                return new double[]{0,0};
            }
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int i = 0; i < matrice.length; i++) {
                for (int j = 0; j < matrice[i].length; j++) {
                    if (!Double.isNaN(matrice[i][j]) && !Double.isInfinite(matrice[i][j])) {
                        min = Math.min(min, matrice[i][j]);
                        max = Math.max(max, matrice[i][j]);
                    }
                }
            }
            if (min == Double.MAX_VALUE) return new double[]{0,0}; // Si toutes les valeurs sont NaN/Inf
            return new double[]{min, max};
        }

        /**
         * Normalise une valeur entre 0 et 1
         */
        private double normaliser(double valeur, double min, double max) {
            if (Double.isNaN(valeur)) return 0.5; // Valeur neutre pour NaN
            if (Math.abs(max - min) < 1e-12) return (valeur > min) ? 1.0 : ( (valeur < min) ? 0.0 : 0.5); // Évite division par zéro
            double norm = (valeur - min) / (max - min);
            return Math.max(0.0, Math.min(1.0, norm)); // Clamp entre 0 et 1
        }

        /**
         * Génère une couleur pour la heatmap (bleu -> cyan -> vert -> jaune -> rouge)
         */
        private Color getCouleurHeatmap(double valeurNormalisee) {
            // Assurer que valeurNormalisee est dans [0, 1]
            float v = (float) Math.max(0.0, Math.min(1.0, valeurNormalisee));

            if (v < 0.25f) { // Bleu à Cyan
                return new Color(0, (int)(255 * (v / 0.25f)), 255);
            } else if (v < 0.5f) { // Cyan à Vert
                return new Color(0, 255, (int)(255 * (1 - (v - 0.25f) / 0.25f)));
            } else if (v < 0.75f) { // Vert à Jaune
                return new Color((int)(255 * ((v - 0.5f) / 0.25f)), 255, 0);
            } else { // Jaune à Rouge
                return new Color(255, (int)(255 * (1 - (v - 0.75f) / 0.25f)), 0);
            }
        }


        /**
         * Génère une couleur spéciale pour les conditions aux limites (plus sombre)
         */
        private Color getCouleurConditionLimite(double valeur, double minVal, double maxVal) {
            double valeurNormalisee = normaliser(valeur, minVal, maxVal);
            Color couleurBase = getCouleurHeatmap(valeurNormalisee);
            return couleurBase.darker(); // Simplement assombrir
        }

        /**
         * Génère une couleur pour la visualisation des erreurs (blanc -> jaune -> orange -> rouge)
         */
        private Color getCouleurErreur(double valeurNormalisee) {
            float v = (float) Math.max(0.0, Math.min(1.0, valeurNormalisee)); // Clamp
            if (v < 0.33f) { // Blanc (255,255,255) à Jaune (255,255,0)
                return new Color(255, 255, (int)(255 * (1 - v/0.33f)));
            } else if (v < 0.66f) { // Jaune (255,255,0) à Orange (255,128,0)
                float t = (v - 0.33f) / 0.33f;
                return new Color(255, (int)(255 * (1-t) + 128 * t), 0);
            } else { // Orange (255,128,0) à Rouge (255,0,0)
                float t = (v - 0.66f) / 0.34f;
                return new Color(255, (int)(128 * (1-t)), 0);
            }
        }

        /**
         * Génère des couleurs distinctes pour les contours
         */
        private Color[] genererCouleursContours(int nbCouleurs) {
            if (nbCouleurs <=0) return new Color[0];
            Color[] couleurs = new Color[nbCouleurs];
            for (int i = 0; i < nbCouleurs; i++) {
                float hue = (float) i / nbCouleurs;
                couleurs[i] = Color.getHSBColor(hue, 0.9f, 0.7f); // Saturation et Brillance ajustées
            }
            return couleurs;
        }

        // Getters et setters
        public void setAfficherGrille(boolean afficher) { this.afficherGrille = afficher; repaint(); }
        public void setAfficherValeurs(boolean afficher) { this.afficherValeurs = afficher; repaint(); }
        public void setAfficherConditionsLimitesTexte(boolean afficher) { this.afficherConditionsLimitesTexte = afficher; repaint(); }
        public void setMettreEnEvidenceBords(boolean evidencer) { this.mettreEnEvidenceBords = evidencer; repaint(); }
        public boolean isAfficherGrille() { return afficherGrille; }
        public boolean isAfficherValeurs() { return afficherValeurs; }
        public boolean isAfficherConditionsLimitesTexte() { return afficherConditionsLimitesTexte; }
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

        JPanel controles = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5); gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; controles.add(new JLabel("Type Visu:"), gbc);
        gbc.gridx = 1;
        JComboBox<TypeVisualisation> comboType = new JComboBox<>(TypeVisualisation.values());
        comboType.setSelectedItem(typeInitial);
        comboType.addActionListener(e -> panel.setTypeVisualisation((TypeVisualisation) comboType.getSelectedItem()));
        controles.add(comboType, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        JCheckBox checkGrille = new JCheckBox("Grille", panel.isAfficherGrille());
        checkGrille.addActionListener(e -> panel.setAfficherGrille(checkGrille.isSelected()));
        controles.add(checkGrille, gbc);

        gbc.gridx = 1;
        JCheckBox checkValeurs = new JCheckBox("Valeurs (si zoom)", panel.isAfficherValeurs());
        checkValeurs.addActionListener(e -> panel.setAfficherValeurs(checkValeurs.isSelected()));
        controles.add(checkValeurs, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        JCheckBox checkInfoCL = new JCheckBox("Infos CL (texte)", panel.isAfficherConditionsLimitesTexte());
        checkInfoCL.addActionListener(e -> panel.setAfficherConditionsLimitesTexte(checkInfoCL.isSelected()));
        controles.add(checkInfoCL, gbc);

        gbc.gridx = 1;
        JCheckBox checkEvidenceBords = new JCheckBox("Évidence bords", panel.isMettreEnEvidenceBords());
        checkEvidenceBords.addActionListener(e -> panel.setMettreEnEvidenceBords(checkEvidenceBords.isSelected()));
        controles.add(checkEvidenceBords, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        JTextArea infoConditionsArea = new JTextArea(3, 30); // Plus petit
        infoConditionsArea.setEditable(false);
        infoConditionsArea.setFont(new Font("Arial", Font.PLAIN, 10));
        infoConditionsArea.setBackground(fenetre.getBackground()); // Couleur du fond de la fenêtre
        infoConditionsArea.setBorder(BorderFactory.createTitledBorder("Infos Conditions Limites"));
        if (maillage != null && maillage.getConditionsLimites() != null) {
            infoConditionsArea.setText(maillage.getConditionsLimites().getDescription());
            infoConditionsArea.append("\nCompatibilité: " +
                (maillage.getConditionsLimites().verifierCompatibilite() ? "OK" : "Problème"));
        }
        controles.add(new JScrollPane(infoConditionsArea), gbc);


        gbc.gridy = 4; gbc.gridwidth = 1;
        JButton boutonExporter = new JButton("Exporter Image");
        boutonExporter.addActionListener(e -> exporterImage(panel));
        controles.add(boutonExporter, gbc);

        gbc.gridx = 1;
        JButton boutonVisuCL = new JButton("Voir CL seules");
        boutonVisuCL.addActionListener(e -> {
            panel.setTypeVisualisation(TypeVisualisation.CONDITIONS_LIMITES);
            comboType.setSelectedItem(TypeVisualisation.CONDITIONS_LIMITES);
        });
        controles.add(boutonVisuCL, gbc);

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
                Graphics2D g2dImg = image.createGraphics();
                panel.paint(g2dImg); // Important: utiliser paint et non paintComponent directement
                g2dImg.dispose();

                java.io.File file = chooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new java.io.File(file.getAbsolutePath() + ".png");
                }
                javax.imageio.ImageIO.write(image, "PNG", file);
                JOptionPane.showMessageDialog(panel, "Image exportée: " + file.getAbsolutePath(),
                    "Exportation Réussie", JOptionPane.INFORMATION_MESSAGE);
            } catch (java.io.IOException e) {
                JOptionPane.showMessageDialog(panel, "Erreur lors de l'export: " + e.getMessage(),
                    "Erreur Exportation", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
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

    // Les méthodes creerPanelComparaisonConditions, creerAnimationConditions, analyseVisuelleConditions
    // nécessiteraient des ajustements similaires pour N_total si elles sont utilisées.
    // Par exemple, dans analyseVisuelleConditions :
    // infoArea.setText("Maillage: " + maillage.getN_total() + "×" + maillage.getN_total() + "\n" + ...);
}
