# Acceso a producción: el formulario, respondido

Google pide rellenar esto al terminar los **14 días de prueba cerrada con 12 testers**. La
prueba arrancó el **30-ago-2026**, así que la ventana se cumple sobre el **13-sep-2026**.

Testers Community mandó su plantilla (`crnicas_production.pdf`) con respuestas ya escritas.
**No se usan tal cual**: dan por hechos cambios que no existían (el botón de valorar, la
navegación arreglada) y hablan de «usuarios objetivo interesados en el fitness» que aquí no
ha reclutado nadie. Lo que sigue es lo mismo, dicho de verdad. Este formulario es lo que más
pesa en la decisión, y contestar lo que no es se paga caro.

---

**1. ¿Cómo reclutaste a los testers? (amigos y familia, un proveedor de pago…)**

> Through a paid testing provider (Testers Community), which recruited 25 testers who
> installed the app from the closed testing track and used it over the 14-day period.

*Es la verdad y es una respuesta perfectamente aceptable: Google pregunta esto para saber si
hubo uso real, no para premiar a quien tiene más amigos.*

---

**2. ¿Fue fácil reclutar testers?**

> Neutral.

*Fácil sería mentir: sin el proveedor de pago no habría reunido 12 personas. Ellos sugieren
«Easy» y no cuesta nada ser exacto.*

---

**3. Describe la participación de los testers durante la prueba cerrada.**

> Testers installed the app on a range of devices and Android versions and used it for the
> full period. They reported no crashes, no ANRs and no functional bugs: every feature
> behaved as intended on every device tested. Their feedback was about the store listing and
> about two things missing inside the app rather than about anything being broken.

---

**4. Resume la respuesta recibida y cómo la recogiste.**

> Feedback arrived as a written report from the testing provider, covering functionality,
> usability and store presence. The findings were:
>
> - No crashes or bugs on any tested device or SDK level.
> - The store screenshots were plain device captures that did not explain any feature.
> - The store description was short and missing the terms people actually search for.
> - There was no way to reach the Play listing to rate the app from inside it.
> - The privacy policy was published for the store but was not reachable from within the app,
>   and there were no terms of use.
> - Pressing back on the home screen closed the app immediately, with no confirmation.

---

**5. ¿A quién va dirigida la app?**

> Anyone who has been asked to keep a food diary, or has decided to keep one, and forgets to
> write in it. It is a notebook with reminders: it does not count calories, does not tell
> anyone what to eat, and does not analyse or score what is written. It is equally useful for
> tracking walks, gym visits and mood notes, and each of those can be switched off.

*Ojo: nada de «health-conscious users» ni «casual dieters» como proponía la plantilla. La app
está en **Estilo de vida**, no en Salud, y la ficha lo sostiene (ver `AVISO-CATEGORIA.md`).
No conviene escribir en un formulario de Google que el público objetivo son personas a dieta.*

---

**6. ¿Qué valor aporta a los usuarios?**

> Writing down what you eat works; remembering to write it down is the hard part. The app
> turns that around: it asks, at the hours the user chooses and as often as they choose, and
> the entry is made in a single tap straight from the notification. Everything stays on the
> phone — there is no account, no server, and the app does not even request internet
> permission — and the user can export what they see as a PDF or an image, or save a full
> backup, at any time.

---

**7. ¿Cuántas instalaciones esperas el primer año?**

> 1k - 10k.

*La plantilla dice 10k-100k. No hay campaña, ni presupuesto, ni una sola reseña todavía: pasar
de mil ya sería mucho. La respuesta no cambia la decisión y no vale la pena inflarla.*

---

**8. ¿Qué cambiaste en la app a partir de la prueba cerrada?**

> Version 2.5 addresses every point raised:
>
> - Added a "Rate this app" entry in Settings, which opens the Play listing. It deliberately
>   does not call the In-App Review API, because Google's own policy states that flow must not
>   be triggered by a button.
> - Added links to the privacy policy and to newly written terms of use inside Settings, so
>   they are reachable without leaving the app.
> - Pressing back on the home screen now shows "Press again to exit" instead of closing
>   immediately.
> - Fixed two localisation defects found while reviewing the app in English: the weekly totals
>   and the calendar's weekday initials were still showing Spanish text.
> - Rewrote the store description around what users search for, and replaced all screenshots
>   with annotated ones that explain each feature. Added a full English store listing, since
>   the app itself has been bilingual since version 2.2.

---

**9. ¿Cómo decidiste que está lista para producción?**

> The closed test found no crashes and no functional defects across the tested devices, and
> every point the testers did raise has been implemented and verified on screen in both
> languages, both themes and at 150% font scale. The app has been in daily personal use for
> over a year, has 47 unit tests covering the reminder grid and the date logic, and has been
> distributed and updated outside Play since July 2026 without incident.

---

**10. ¿Qué has hecho distinto esta vez?**

> This was the first release cycle driven by feedback from people other than me. Two of the
> issues — no way to rate the app, and no legal texts inside it — were invisible from the
> inside, because I already knew where everything was.

---

## Antes de enviarlo

- [ ] Haber subido a Play la **2.5**, con los cambios que la respuesta 8 dice que existen.
- [ ] Ficha actualizada con `FICHA.md` (descripción larga y corta, y título).
- [ ] Capturas nuevas subidas (`graficos/es/`), y la ficha en inglés creada si se quiere.
- [ ] Comprobar en Play Console que los 12 testers siguen apuntados: si alguno se sale antes
      de los 14 días, la cuenta se rompe y el formulario no aparece.
