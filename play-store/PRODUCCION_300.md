# Acceso a producción — respuestas recortadas a 300 caracteres

El formulario de Play Console limita **cada respuesta a 300 caracteres** y no lo avisa hasta
que escribes. Estas son las mismas respuestas de `PRODUCCION.md`, recortadas para que entren.
Copiar y pegar tal cual.

## Paso 1 — Información sobre tu prueba cerrada

**¿Cómo reclutaste usuarios?**

```
Through a paid testing provider, Testers Community. They recruited 25 testers who joined the closed testing track, installed the app themselves and used it over the full 14-day period.
```

**¿Cómo de fácil te ha resultado reclutar testers?** → **Ni difícil ni fácil**

**Describe las interacciones de los testers**

```
Testers used every feature on a range of devices and Android versions, consistently across the 14 days: reminders, logging entries, photos, the day, week and month views, filters, export and backup. No crashes, ANRs or functional bugs were reported on any device.
```

**Resume los comentarios y cómo los recogiste**

```
Collected as a written report from the provider. No bugs found. The points raised: screenshots and description did not explain the features; no way to open the Play listing to rate the app; privacy policy and terms not reachable inside the app; back on the home screen closed it with no warning.
```

## Paso 2 — Sobre tu aplicación

**¿A quién va dirigida?**

```
Anyone who has been asked to keep a food diary, or decided to keep one, and forgets to write in it. It is a notebook with reminders: it does not count calories, tell anyone what to eat, or score what is written. Walks, gym visits and mood notes can be logged too, and each can be switched off.
```

**¿Qué valor aporta?**

```
Writing down what you eat works; remembering to do it is the hard part. The app asks at the hours you choose, and the entry takes one tap from the notification. Everything stays on the phone: no account, no server, no internet permission. Entries export as PDF or image, with full backup.
```

**Instalaciones esperadas el primer año** → **1k - 10k**

## Paso 3 — Preparación para producción

**¿Qué cambiaste a partir de la prueba cerrada?**

```
Version 2.5.1 addresses every point: a Rate this app entry in Settings that opens the Play listing; privacy policy and new terms of use reachable inside the app; press back twice to exit; two English translation defects fixed; rewritten description and annotated screenshots in both languages.
```

**¿Cómo decidiste que está lista?**

```
The closed test found no crashes and no functional defects, and every point raised has been implemented and checked on screen in both languages, both themes and at 150% font scale. The app has 47 unit tests and has been in daily use, distributed outside Play since July 2026, without incident.
```

**¿Qué has hecho distinto esta vez?**

```
This was the first release cycle driven by feedback from people other than me. Two of the issues, no way to rate the app and no legal texts inside it, were invisible from the inside because I already knew where everything was.
```
