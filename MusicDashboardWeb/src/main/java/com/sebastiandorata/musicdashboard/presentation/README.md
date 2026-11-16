[← Back to root](../../../../../../../README.md)

# presentation

## Behavior

Groups the view models that turn services' data into what a page needs: `Analytics/` (stat cards, top lists, listening history and reports), `graph/` (the dashboard line-chart series) and `libraryViews/` (library page state). The folder itself contains only these subfolders.

## Responsibilities

- Owns view-model packages that sit between controllers and services.
- Does not hold controllers, HTML or persistence code, and no class belongs directly in this folder.

## S.O.L.I.D. principles used

Not applicable: this folder holds no logic of its own, so none of the five principles can be shown here.

## Code reused

None; reuse happens in the subfolders.

## Table of contents

- [Analytics/](Analytics/README.md)
- [graph/](graph/README.md)
- [libraryViews/](libraryViews/README.md)
- [← Back to root](../../../../../../../README.md)

## Sequence diagram

```mermaid
sequenceDiagram
    participant C as controller
    participant A as presentation/Analytics
    participant G as presentation/graph
    participant L as presentation/libraryViews
    participant S as service
    C->>A: request stat cards, top lists, history
    C->>G: request chart series
    C->>L: build library state
    A->>S: fetch data
    G->>S: fetch data
    A-->>C: DTOs
    G-->>C: ChartData
```
