/**
 * Classe pour la gestion des conditions aux limites de Dirichlet générales
 *
 * Cette classe permet de spécifier des conditions aux limites de type :
 * - U(x,0) = g₀(x) sur le bord inférieur
 * - U(x,1) = g₁(x) sur le bord supérieur
 * - U(0,y) = h₀(y) sur le bord gauche
 * - U(1,y) = h₁(y) sur le bord droit
 *
 * Les conditions peuvent être constantes ou variables selon la position.
 */
public class ConditionsLimites {

    public ConditionsLimites(String s, int i, int i1, int i2, int i3, String description) {
    }

    /**
     * Interface fonctionnelle pour définir une fonction de condition aux limites
     */
    @FunctionalInterface
    public interface FonctionBord {
        /**
         * Calcule la valeur de la condition aux limites
         * @param t Paramètre le long du bord (entre 0 et 1)
         * @return Valeur de U sur le bord
         */
        double evaluer(double t);
    }

    /**
     * Énumération des types de conditions aux limites prédéfinies
     */
    public enum TypeCondition {
        HOMOGENES("Homogènes (U = 0 sur tous les bords)"),
        CONSTANTES("Constantes (valeurs spécifiées)"),
        LINEAIRES("Variations linéaires"),
        SINUSOIDALES("Variations sinusoïdales"),
        POLYNOMIALES("Variations polynomiales"),
        PERSONNALISEES("Fonctions personnalisées");

        private final String description;

        TypeCondition(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    // Fonctions définissant les conditions sur chaque bord
    private FonctionBord bordInferieur;  // y = 0, U(x,0) = g₀(x)
    private FonctionBord bordSuperieur;  // y = 1, U(x,1) = g₁(x)
    private FonctionBord bordGauche;     // x = 0, U(0,y) = h₀(y)
    private FonctionBord bordDroit;      // x = 1, U(1,y) = h₁(y)

    // Valeurs constantes (utilisées pour les conditions constantes)
    private double valeurInferieure = 0.0;
    private double valeurSuperieure = 0.0;
    private double valeurGauche = 0.0;
    private double valeurDroite = 0.0;

    private TypeCondition type;
    private String description;

    /**
     * Constructeur par défaut - conditions homogènes
     */
    public ConditionsLimites() {
        this.type = TypeCondition.HOMOGENES;
        this.description = "Conditions homogènes U = 0";
        configurerConditionsHomogenes();
    }

    /**
     * Constructeur pour conditions constantes
     */
    public ConditionsLimites(double valInf, double valSup, double valGauche, double valDroite) {
        this.type = TypeCondition.CONSTANTES;
        this.valeurInferieure = valInf;
        this.valeurSuperieure = valSup;
        this.valeurGauche = valGauche;
        this.valeurDroite = valDroite;
        this.description = String.format("Conditions constantes: Inf=%.2f, Sup=%.2f, G=%.2f, D=%.2f",
                valInf, valSup, valGauche, valDroite);
        configurerConditionsConstantes();
    }

    /**
     * Constructeur pour conditions personnalisées
     */
    public ConditionsLimites(FonctionBord bordInf, FonctionBord bordSup,
                             FonctionBord bordGauche, FonctionBord bordDroit, String desc) {
        this.type = TypeCondition.PERSONNALISEES;
        this.bordInferieur = bordInf;
        this.bordSuperieur = bordSup;
        this.bordGauche = bordGauche;
        this.bordDroit = bordDroit;
        this.description = desc;
    }

    /**
     * Configure des conditions aux limites homogènes (U = 0 partout)
     */
    public void configurerConditionsHomogenes() {
        bordInferieur = t -> 0.0;
        bordSuperieur = t -> 0.0;
        bordGauche = t -> 0.0;
        bordDroit = t -> 0.0;

        valeurInferieure = valeurSuperieure = valeurGauche = valeurDroite = 0.0;
        type = TypeCondition.HOMOGENES;
        description = "Conditions homogènes U = 0";

        // System.out.println("Conditions aux limites configurées: " + description); // Commenté pour moins de verbosité
    }

    /**
     * Configure des conditions aux limites constantes
     */
    public void configurerConditionsConstantes() {
        bordInferieur = t -> valeurInferieure;
        bordSuperieur = t -> valeurSuperieure;
        bordGauche = t -> valeurGauche;
        bordDroit = t -> valeurDroite;

        // System.out.println("Conditions aux limites configurées: " + description); // Commenté
    }

    /**
     * Configure des conditions aux limites linéaires
     * Les valeurs varient linéairement le long de chaque bord
     */
    public void configurerConditionsLineaires(double coin00, double coin10, double coin01, double coin11) {
        // coin00 = U(0,0), coin10 = U(1,0), coin01 = U(0,1), coin11 = U(1,1)

        bordInferieur = t -> coin00 + t * (coin10 - coin00);  // y=0: de (0,0) à (1,0) ; t = x
        bordSuperieur = t -> coin01 + t * (coin11 - coin01);  // y=1: de (0,1) à (1,1) ; t = x
        bordGauche = t -> coin00 + t * (coin01 - coin00);     // x=0: de (0,0) à (0,1) ; t = y
        bordDroit = t -> coin10 + t * (coin11 - coin10);      // x=1: de (1,0) à (1,1) ; t = y

        type = TypeCondition.LINEAIRES;
        description = String.format("Conditions linéaires: coins (%.2f,%.2f,%.2f,%.2f)",
                coin00, coin10, coin01, coin11);

        // System.out.println("Conditions aux limites configurées: " + description); // Commenté
    }

    /**
     * Configure des conditions aux limites sinusoïdales
     */
    public void configurerConditionsSinusoidales(double amplitude, int frequence) {
        bordInferieur = t -> amplitude * Math.sin(frequence * Math.PI * t);
        bordSuperieur = t -> amplitude * Math.sin(frequence * Math.PI * t);
        bordGauche = t -> amplitude * Math.sin(frequence * Math.PI * t);
        bordDroit = t -> amplitude * Math.sin(frequence * Math.PI * t);

        type = TypeCondition.SINUSOIDALES;
        description = String.format("Conditions sinusoïdales: A=%.2f, f=%d", amplitude, frequence);

        // System.out.println("Conditions aux limites configurées: " + description); // Commenté
    }

    /**
     * Configure des conditions aux limites polynomiales
     */
    public void configurerConditionsPolynomiales() {
        bordInferieur = t -> t * (1 - t);          // Parabole s'annulant aux coins
        bordSuperieur = t -> 0.5 * t * (1 - t);    // Parabole plus faible
        bordGauche = t -> t * t;                   // Croissance quadratique
        bordDroit = t -> (1 - t) * (1 - t);       // Décroissance quadratique

        type = TypeCondition.POLYNOMIALES;
        description = "Conditions polynomiales (exemples quadratiques)";

        // System.out.println("Conditions aux limites configurées: " + description); // Commenté
    }

    /**
     * Applique les conditions aux limites à un maillage
     *
     * @param U Matrice de la solution
     * @param N_total Taille totale du maillage (nombre de points, bords inclus)
     * @param h Pas du maillage
     */
    public void appliquerConditions(double[][] U, int N_total, double h) {
        // Bord inférieur y = 0 (i = 0)
        // j va de 0 à N_total-1
        for (int j = 0; j < N_total; j++) {
            double x = j * h; // x va de 0 à 1
            U[0][j] = bordInferieur.evaluer(x);
        }

        // Bord supérieur y = 1 (i = N_total-1)
        // j va de 0 à N_total-1
        for (int j = 0; j < N_total; j++) {
            double x = j * h; // x va de 0 à 1
            U[N_total - 1][j] = bordSuperieur.evaluer(x);
        }

        // Bord gauche x = 0 (j = 0)
        // i va de 0 à N_total-1
        for (int i = 0; i < N_total; i++) {
            double y = i * h; // y va de 0 à 1
            U[i][0] = bordGauche.evaluer(y);
        }

        // Bord droit x = 1 (j = N_total-1)
        // i va de 0 à N_total-1
        for (int i = 0; i < N_total; i++) {
            double y = i * h; // y va de 0 à 1
            U[i][N_total - 1] = bordDroit.evaluer(y);
        }
    }

    /**
     * Vérifie si les conditions aux limites sont compatibles aux coins
     * Les valeurs aux coins doivent être cohérentes entre les bords adjacents
     */
    public boolean verifierCompatibilite() {
        double tolerance = 1e-10;

        // Coin (0,0) -> x=0, y=0
        double coin00_gauche = bordGauche.evaluer(0.0);    // h0(0)
        double coin00_inf = bordInferieur.evaluer(0.0);  // g0(0)
        if (Math.abs(coin00_gauche - coin00_inf) > tolerance) {
            System.err.println("Incompatibilité au coin (0,0) [i=0,j=0]: G=" + coin00_gauche + ", Inf=" + coin00_inf);
            return false;
        }

        // Coin (1,0) -> x=1, y=0
        double coin10_droit = bordDroit.evaluer(0.0);      // h1(0)
        double coin10_inf = bordInferieur.evaluer(1.0);    // g0(1)
        if (Math.abs(coin10_droit - coin10_inf) > tolerance) {
            System.err.println("Incompatibilité au coin (1,0) [i=0,j=N-1]: D=" + coin10_droit + ", Inf=" + coin10_inf);
            return false;
        }

        // Coin (0,1) -> x=0, y=1
        double coin01_gauche = bordGauche.evaluer(1.0);    // h0(1)
        double coin01_sup = bordSuperieur.evaluer(0.0);  // g1(0)
        if (Math.abs(coin01_gauche - coin01_sup) > tolerance) {
            System.err.println("Incompatibilité au coin (0,1) [i=N-1,j=0]: G=" + coin01_gauche + ", Sup=" + coin01_sup);
            return false;
        }

        // Coin (1,1) -> x=1, y=1
        double coin11_droit = bordDroit.evaluer(1.0);      // h1(1)
        double coin11_sup = bordSuperieur.evaluer(1.0);    // g1(1)
        if (Math.abs(coin11_droit - coin11_sup) > tolerance) {
            System.err.println("Incompatibilité au coin (1,1) [i=N-1,j=N-1]: D=" + coin11_droit + ", Sup=" + coin11_sup);
            return false;
        }

        return true;
    }

    /**
     * Calcule la valeur de condition aux limites en un point du bord (indices globaux i,j)
     * @param i Indice de ligne global (0 à N_total-1)
     * @param j Indice de colonne global (0 à N_total-1)
     * @param N_total Nombre total de points dans une direction
     * @param h Pas du maillage
     * @return Valeur de la condition limite
     */
    public double getValeurBord(int i, int j, int N_total, double h) {
        if (i == 0) { // Bord inférieur (y=0)
            return bordInferieur.evaluer(j * h); // x = j*h
        } else if (i == N_total - 1) { // Bord supérieur (y=1)
            return bordSuperieur.evaluer(j * h); // x = j*h
        } else if (j == 0) { // Bord gauche (x=0)
            return bordGauche.evaluer(i * h);    // y = i*h
        } else if (j == N_total - 1) { // Bord droit (x=1)
            return bordDroit.evaluer(i * h);     // y = i*h
        }
        // Ce cas ne devrait pas arriver si appelé depuis Maillage.getValeurConditionLimite
        // qui vérifie déjà estSurBord.
        throw new IllegalArgumentException("Point (" + i + "," + j + ") n'est pas sur le bord pour N_total=" + N_total);
    }

    /**
     * Retourne une description textuelle des conditions
     */
    public String getDescription() {
        return description;
    }

    public TypeCondition getType() {
        return type;
    }

    // Getters pour les valeurs constantes
    public double getValeurInferieure() { return valeurInferieure; }
    public double getValeurSuperieure() { return valeurSuperieure; }
    public double getValeurGauche() { return valeurGauche; }
    public double getValeurDroite() { return valeurDroite; }

    // Setters pour les valeurs constantes
    public void setValeurInferieure(double val) {
        this.valeurInferieure = val;
        if (type == TypeCondition.CONSTANTES) configurerConditionsConstantes();
    }
    public void setValeurSuperieure(double val) {
        this.valeurSuperieure = val;
        if (type == TypeCondition.CONSTANTES) configurerConditionsConstantes();
    }
    public void setValeurGauche(double val) {
        this.valeurGauche = val;
        if (type == TypeCondition.CONSTANTES) configurerConditionsConstantes();
    }
    public void setValeurDroite(double val) {
        this.valeurDroite = val;
        if (type == TypeCondition.CONSTANTES) configurerConditionsConstantes();
    }

    /**
     * Crée des conditions aux limites prédéfinies pour les cas de test
     */
    public static ConditionsLimites creerConditionsTest(String nom) {
        switch (nom.toLowerCase()) {
            case "homogenes":
                return new ConditionsLimites();

            case "constantes_unitaires":
                return new ConditionsLimites(1.0, 1.0, 1.0, 1.0);

            case "constantes_variables":
                return new ConditionsLimites(0.0, 1.0, 0.5, 0.8);

            case "lineaires":
                ConditionsLimites cl = new ConditionsLimites();
                cl.configurerConditionsLineaires(0.0, 1.0, 0.0, 1.0); // U(0,0)=0, U(1,0)=1, U(0,1)=0, U(1,1)=1
                return cl;

            case "sinusoidales":
                ConditionsLimites cs = new ConditionsLimites();
                cs.configurerConditionsSinusoidales(0.5, 1); // Amplitude 0.5, fréquence 1
                return cs;

            case "polynomiales":
                ConditionsLimites cp = new ConditionsLimites();
                cp.configurerConditionsPolynomiales();
                return cp;

            default:
                System.err.println("Conditions inconnues: " + nom + ". Utilisation des conditions homogènes.");
                return new ConditionsLimites();
        }
    }

    @Override
    public String toString() {
        return description;
    }
}
