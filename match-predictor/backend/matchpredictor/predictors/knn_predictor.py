from typing import List, Tuple, Optional

import numpy as np
from numpy import float64
from numpy.typing import NDArray
from sklearn.neighbors import KNeighborsClassifier  # type: ignore
from sklearn.preprocessing import OneHotEncoder  # type: ignore

from matchpredictor.matchresults.result import Fixture, Outcome, Result, Team
from matchpredictor.predictors.predictor import Predictor, Prediction


class KnnPredictor(Predictor):
    def __init__(self, model: KNeighborsClassifier, team_encoding: OneHotEncoder) -> None:
        self.model = model
        self.team_encoding = team_encoding

    def predict(self, fixture: Fixture) -> Prediction:
        encoded_home_name = self.__encode_team(fixture.home_team)
        encoded_away_name = self.__encode_team(fixture.away_team)

        if encoded_home_name is None:
            return Prediction(outcome=Outcome.AWAY)
        if encoded_away_name is None:
            return Prediction(outcome=Outcome.HOME)

        x: NDArray[float64] = np.concatenate([encoded_home_name, encoded_away_name], 1)  # type: ignore
        pred = self.model.predict(x)

        if pred > 0:
            return Prediction(outcome=Outcome.HOME)
        elif pred < 0:
            return Prediction(outcome=Outcome.AWAY)
        else:
            return Prediction(outcome=Outcome.DRAW)

    def __encode_team(self, team: Team) -> Optional[NDArray[float64]]:
        try:
            return self.team_encoding.transform(np.array(team.name).reshape(-1, 1))  # type: ignore
        except ValueError:
            return None


def build_model(results: List[Result]) -> Tuple[KNeighborsClassifier, OneHotEncoder]:
    home_names = np.array([r.fixture.home_team.name for r in results])
    away_names = np.array([r.fixture.away_team.name for r in results])
    home_goals = np.array([r.home_goals for r in results])
    away_goals = np.array([r.away_goals for r in results])

    team_names = np.array(list(home_names) + list(away_names)).reshape(-1, 1)
    team_encoding = OneHotEncoder(sparse=False).fit(team_names)

    encoded_home_names = team_encoding.transform(home_names.reshape(-1, 1))
    encoded_away_names = team_encoding.transform(away_names.reshape(-1, 1))

    x: NDArray[float64] = np.concatenate([encoded_home_names, encoded_away_names], 1)  # type: ignore
    y = np.sign(home_goals - away_goals)

    model = KNeighborsClassifier(n_neighbors=30)
    model.fit(x, y)

    return model, team_encoding


def train_knn_predictor(results: List[Result]) -> Predictor:
    model, team_encoding = build_model(results)

    return KnnPredictor(model, team_encoding)
