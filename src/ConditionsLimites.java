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

        System.out.println("Conditions aux limites configurées: " + description);
    }

    /**
     * Configure des conditions aux limites constantes
     */
    public void configurerConditionsConstantes() {
        bordInferieur = t -> valeurInferieure;
        bordSuperieur = t -> valeurSuperieure;
        bordGauche = t -> valeurGauche;
        bordDroit = t -> valeurDroite;

        System.out.println("Conditions aux limites configurées: " + description);
    }

    /**
     * Configure des conditions aux limites linéaires
     * Les valeurs varient linéairement le long de chaque bord
     */
    public void configurerConditionsLineaires(double coin00, double coin10, double coin01, double coin11) {
        // coin00 = U(0,0), coin10 = U(1,0), coin01 = U(0,1), coin11 = U(1,1)

        bordInferieur = t -> coin00 + t * (coin10 - coin00);  // y=0: de (0,0) à (1,0)
        bordSuperieur = t -> coin01 + t * (coin11 - coin01);  // y=1: de (0,1) à (1,1)
        bordGauche = t -> coin00 + t * (coin01 - coin00);     // x=0: de (0,0) à (0,1)
        bordDroit = t -> coin10 + t * (coin11 - coin10);      // x=1: de (1,0) à (1,1)

        type = TypeCondition.LINEAIRES;
        description = String.format("Conditions linéaires: coins (%.2f,%.2f,%.2f,%.2f)",
            coin00, coin10, coin01, coin11);

        System.out.println("Conditions aux limites configurées: " + description);
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

        System.out.println("Conditions aux limites configurées: " + description);
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

        System.out.println("Conditions aux limites configurées: " + description);
    }

    /**
     * Applique les conditions aux limites à un maillage
     *
     * @param U Matrice de la solution
     * @param N Taille du maillage intérieur
     * @param h Pas du maillage
     */
    public void appliquerConditions(double[][] U, int N, double h) {
        // Bord inférieur y = 0 (i = 0)
        for (int j = 0; j <= N + 1; j++) {
            double x = j * h;
            U[0][j] = bordInferieur.evaluer(x);
        }

        // Bord supérieur y = 1 (i = N+1)
        for (int j = 0; j <= N + 1; j++) {
            double x = j * h;
            U[N + 1][j] = bordSuperieur.evaluer(x);
        }

        // Bord gauche x = 0 (j = 0)
        for (int i = 0; i <= N + 1; i++) {
            double y = i * h;
            U[i][0] = bordGauche.evaluer(y);
        }

        // Bord droit x = 1 (j = N+1)
        for (int i = 0; i <= N + 1; i++) {
            double y = i * h;
            U[i][N + 1] = bordDroit.evaluer(y);
        }
    }

    /**
     * Vérifie si les conditions aux limites sont compatibles aux coins
     * Les valeurs aux coins doivent être cohérentes entre les bords adjacents
     */
    public boolean verifierCompatibilite() {
        double tolerance = 1e-10;

        // Coin (0,0)
        double coin00_gauche = bordGauche.evaluer(0.0);
        double coin00_inf = bordInferieur.evaluer(0.0);
        if (Math.abs(coin00_gauche - coin00_inf) > tolerance) {
            System.err.println("Incompatibilité au coin (0,0): " + coin00_gauche + " ≠ " + coin00_inf);
            return false;
        }

        // Coin (1,0)
        double coin10_droit = bordDroit.evaluer(0.0);
        double coin10_inf = bordInferieur.evaluer(1.0);
        if (Math.abs(coin10_droit - coin10_inf) > tolerance) {
            System.err.println("Incompatibilité au coin (1,0): " + coin10_droit + " ≠ " + coin10_inf);
            return false;
        }

        // Coin (0,1)
        double coin01_gauche = bordGauche.evaluer(1.0);
        double coin01_sup = bordSuperieur.evaluer(0.0);
        if (Math.abs(coin01_gauche - coin01_sup) > tolerance) {
            System.err.println("Incompatibilité au coin (0,1): " + coin01_gauche + " ≠ " + coin01_sup);
            return false;
        }

        // Coin (1,1)
        double coin11_droit = bordDroit.evaluer(1.0);
        double coin11_sup = bordSuperieur.evaluer(1.0);
        if (Math.abs(coin11_droit - coin11_sup) > tolerance) {
            System.err.println("Incompatibilité au coin (1,1): " + coin11_droit + " ≠ " + coin11_sup);
            return false;
        }

        return true;
    }

    /**
     * Calcule la valeur de condition aux limites en un point du bord
     */
    public double getValeurBord(int i, int j, int N, double h) {
        if (i == 0) {
            // Bord inférieur
            return bordInferieur.evaluer(j * h);
        } else if (i == N + 1) {
            // Bord supérieur
            return bordSuperieur.evaluer(j * h);
        } else if (j == 0) {
            // Bord gauche
            return bordGauche.evaluer(i * h);
        } else if (j == N + 1) {
            // Bord droit
            return bordDroit.evaluer(i * h);
        }

        throw new IllegalArgumentException("Point (" + i + "," + j + ") n'est pas sur le bord");
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
                cl.configurerConditionsLineaires(0.0, 1.0, 0.0, 1.0);
                return cl;

            case "sinusoidales":
                ConditionsLimites cs = new ConditionsLimites();
                cs.configurerConditionsSinusoidales(0.5, 1);
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
