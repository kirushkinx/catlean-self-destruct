## CatLean Self-Destruct
Kill switch addon for the [CatLean](https://catlean.su/)

### About
On trigger the client stops existing inside the running game: nothing ticks, nothing renders, no key is read, no packet is touched. Only a game restart brings it back.

### Usage
Category `Utility` -> `Misc` -> `Self Destruct`  or the `^selfdestruct` command.

| Setting      | Default   | Effect                                          |
|--------------|-----------|-------------------------------------------------|
| `Trigger`    | `Instant` | `Confirm` makes the first toggle arm only       |
| `Confirm Ms` | `10000`   | confirmation window for `Confirm`               |
| `Silent`     | `true`    | leave nothing behind - no chat, no console line |


