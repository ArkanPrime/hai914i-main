import os
import pandas as pd
import matplotlib
matplotlib.use("Agg") # Mode sans interface graphique (idéal pour WSL/Serveur)
import matplotlib.pyplot as plt
import numpy as np

# Configuration des fichiers à analyser
FILES_TO_ANALYZE = [
    "benchmark_compare_all.csv",
    "benchmark_compare_10000.csv"
]

def analyze_file(filename):
    if not os.path.exists(filename):
        print(f"⚠️  Fichier introuvable : {filename} (Passé)")
        return

    print(f"\n--- Analyse de {filename} ---")
    
    # 1. Chargement robuste : on ignore les lignes commençant par '#'
    try:
        df = pd.read_csv(filename, comment='#', skipinitialspace=True)
    except Exception as e:
        print(f"❌ Erreur critique à la lecture de {filename}: {e}")
        return

    # Nettoyage des noms de colonnes
    df.columns = df.columns.str.strip()
    print(f"   Colonnes détectées : {list(df.columns)}")

    # Vérification des colonnes nécessaires
    required_cols = {'engine', 'answers', 'exec_ms'}
    if not required_cols.issubset(df.columns):
        print(f"❌ Colonnes manquantes. Attendu: {required_cols}")
        return

    # Création d'une colonne pour distinguer "0 réponses" vs "Avec réponses"
    df['type_reponse'] = df['answers'].apply(lambda x: '0 Réponses' if x == 0 else '>0 Réponses')

    # --- Graphique 1 : Nombre de requêtes (Comparaison par Engine) ---
    # On compte combien de requêtes tombent dans chaque catégorie pour chaque moteur
    summary_counts = df.groupby(['engine', 'type_reponse']).size().unstack(fill_value=0)
    
    ax = summary_counts.plot(kind='bar', figsize=(8, 6), rot=0, color=['#ff9999', '#66b3ff'])
    plt.title(f"Répartition des réponses par Moteur\n({filename})")
    plt.ylabel("Nombre de requêtes")
    plt.xlabel("Moteur")
    plt.legend(title="Type de résultat")
    
    # Ajouter les valeurs sur les barres
    for container in ax.containers:
        ax.bar_label(container)

    plot_name = filename.replace(".csv", "_repartition.png")
    plt.savefig(plot_name)
    plt.close()
    print(f"✅ Graphique 1 généré : {plot_name}")


    # --- Graphique 2 : Temps moyen (Comparaison par Engine) ---
    # On calcule la moyenne de 'exec_ms' pour chaque groupe
    summary_means = df.groupby(['engine', 'type_reponse'])['exec_ms'].mean().unstack(fill_value=0)

    ax = summary_means.plot(kind='bar', figsize=(8, 6), rot=0, color=['#ffcc99', '#99ff99'])
    plt.title(f"Temps d'exécution MOYEN par Moteur\n({filename})")
    plt.ylabel("Temps moyen (ms)")
    plt.xlabel("Moteur")
    plt.legend(title="Type de résultat")

    # Ajouter les valeurs arrondies
    for container in ax.containers:
        ax.bar_label(container, fmt='%.2f')

    plot_name = filename.replace(".csv", "_temps_moyen.png")
    plt.savefig(plot_name)
    plt.close()
    print(f"✅ Graphique 2 généré : {plot_name}")


    # --- Graphique 3 : Distribution des temps (Uniquement pour >0 réponses) ---
    df_answers = df[df['answers'] > 0]

    if not df_answers.empty:
        plt.figure(figsize=(10, 6))
        
        # On trace un histogramme pour chaque moteur
        engines = df_answers['engine'].unique()
        for engine in engines:
            subset = df_answers[df_answers['engine'] == engine]
            plt.hist(subset['exec_ms'], bins=50, alpha=0.6, label=engine, edgecolor='black')

        plt.title(f"Distribution des temps d'exécution (>0 réponses)\n({filename})")
        plt.xlabel("Temps (ms)")
        plt.ylabel("Nombre de requêtes")
        plt.legend()
        plt.grid(axis='y', alpha=0.3)
        
        # Échelle log si les différences sont trop grandes (optionnel, décommentez si besoin)
        # plt.yscale('log') 

        plot_name = filename.replace(".csv", "_distrib.png")
        plt.savefig(plot_name)
        plt.close()
        print(f"✅ Graphique 3 généré : {plot_name}")
    else:
        print("⚠️ Pas de données avec réponses pour tracer la distribution.")

def main():
    print("=== Démarrage de l'analyse ===")
    for f in FILES_TO_ANALYZE:
        analyze_file(f)
    print("\n=== Terminé ===")

if __name__ == "__main__":
    main()