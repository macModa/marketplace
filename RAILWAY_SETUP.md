# Configuration Railway pour corriger les erreurs 403

Si vous obtenez toujours une erreur 403 après avoir déployé le code, vérifiez ces points sur Railway :

## 1. Variables d'environnement
Le backend utilise la variable `JWT_SECRET` pour valider les tokens. Si elle n'est pas définie, il utilise une valeur par défaut.
**Important :** Si votre application Flutter utilise un secret différent, la validation échouera.

### Comment vérifier :
1. Allez sur votre tableau de bord Railway.
2. Sélectionnez votre service.
3. Cliquez sur l'onglet **Variables**.
4. Assurez-vous que `JWT_SECRET` est présent et correspond à celui utilisé pour générer vos tokens.

### En ligne de commande (Railway CLI) :
```bash
railway variables
```

## 2. Base de données Production
Le backend recherche l'utilisateur par son email dans la base de données Railway (`userDetailsService.loadUserByUsername`).
Si l'utilisateur n'existe pas dans la base de données de **production**, vous aurez une erreur 403.

## 3. Test avec CURL
Testez avec un nouveau token généré après déploiement :

```bash
curl -X GET \
  https://marketplace-production-45a2.up.railway.app/api/orders/my-orders \
  -H "Authorization: Bearer VOTRE_NOUVEAU_TOKEN" \
  -v
```

Si vous voyez `403 Forbidden`, regardez les logs Railway :
```bash
railway logs
```
Cherchez les messages : 
- `Token validated for user: ...`
- `Loaded user: ... with authorities: [...]`
- `Token validation failed for JWT: ...` (Si vous voyez ça, c'est le `JWT_SECRET` qui est mauvais).
