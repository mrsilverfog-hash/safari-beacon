# Safari Beacon — Mod Fabric 1.21.1

Affiche un **faisceau lumineux rouge** montant au ciel au-dessus de chaque bloc
`Gravier Suspect Safari` et `Sable Suspect Safari` du mod TropiFurnitures.

## Prérequis

- Java 21 (JDK) : https://adoptium.net/
- IntelliJ IDEA (recommandé) : https://www.jetbrains.com/idea/

## Compilation

### Avec IntelliJ IDEA (recommandé)
1. Ouvrir IntelliJ IDEA
2. `File > Open` → sélectionner ce dossier `safari-beacon`
3. Attendre que Gradle télécharge les dépendances
4. Dans le panneau Gradle (à droite) : `safari-beacon > Tasks > build > build`
5. Le `.jar` se trouve dans `build/libs/safari-beacon-1.0.0.jar`

### En ligne de commande
```bash
# Windows
gradlew.bat build

# Linux / macOS
chmod +x gradlew
./gradlew build
```

## Installation

1. Copier `build/libs/safari-beacon-1.0.0.jar` dans votre dossier `.minecraft/mods/`
2. S'assurer que **Fabric Loader** et **Fabric API** sont installés
3. Lancer Minecraft 1.21.1 avec le profil Fabric

## Configuration

Dans `SafariBeaconRenderer.java` vous pouvez modifier :
- `SEARCH_RADIUS` : rayon de détection autour du joueur (défaut : 48 blocs)
- `BEAM_HEIGHT` : hauteur du faisceau (défaut : 256 blocs)
- `BEAM_RED/GREEN/BLUE/ALPHA` : couleur du faisceau
- `BEAM_INNER_RADIUS` / `BEAM_OUTER_RADIUS` : épaisseur du faisceau
